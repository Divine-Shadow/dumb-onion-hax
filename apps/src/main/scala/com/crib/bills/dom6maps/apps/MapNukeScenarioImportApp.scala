package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}

import scala.xml.XML
import scala.util.Try
import java.nio.file.{Files as JavaFiles, Path as NioPath}

object MapNukeScenarioImportApp extends IOApp:
  private final case class SpawnPoint(x: Int, y: Int, spawnType: String)
  private final case class LegacyLayout(name: String, xSize: Int, ySize: Int, spawns: Vector[SpawnPoint])

  override def run(args: List[String]): IO[ExitCode] =
    parseArguments(args) match
      case Left(error) =>
        IO.raiseError(IllegalArgumentException(error))
      case Right(config) =>
        for
          xmlText <- IO(JavaFiles.readString(config.layoutsPath))
          layouts <- IO.fromEither(parseLayouts(xmlText))
          selectedLayout <- IO.fromEither(
            layouts.find(_.name == config.layoutName).toRight(
              IllegalArgumentException(s"Layout not found: ${config.layoutName}")
            )
          )
          scenarioText <- IO.fromEither(renderScenario(selectedLayout, config))
          _ <- IO(JavaFiles.createDirectories(config.outputPath.getParent))
          _ <- IO(JavaFiles.writeString(config.outputPath, scenarioText))
          _ <- IO.println(s"Wrote scenario catalog: ${config.outputPath}")
        yield ExitCode.Success

  private final case class ImportConfig(
      layoutsPath: NioPath,
      outputPath: NioPath,
      layoutName: String,
      scenarioId: String,
      nationIds: Vector[Int],
      allocationProfileCatalogPath: String
  )

  private def parseArguments(args: List[String]): Either[String, ImportConfig] =
    val keyValue = args.flatMap { argument =>
      argument.split("=", 2).toList match
        case key :: value :: Nil if key.startsWith("--") => Some(key.drop(2) -> value)
        case _ => None
    }.toMap

    for
      layoutsPath <- keyValue.get("layouts").toRight("Missing --layouts=<path>")
      outputPath <- keyValue.get("output").toRight("Missing --output=<path>")
      layoutName <- keyValue.get("layout-name").toRight("Missing --layout-name=<name>")
      scenarioId <- keyValue.get("scenario-id").toRight("Missing --scenario-id=<id>")
      nationIdsText <- keyValue.get("nation-ids").toRight("Missing --nation-ids=5,41,12")
      nationIds <- nationIdsText
        .split(",")
        .toVector
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(text => text.toIntOption.toRight(s"Invalid nation id: $text"))
        .foldLeft(Right(Vector.empty): Either[String, Vector[Int]]) { (accumulator, current) =>
          for
            built <- accumulator
            next <- current
          yield built :+ next
        }
      allocationProfileCatalogPath = keyValue.getOrElse("allocation-profile-catalog-path", "data/map-generation/allocation-profiles.conf")
    yield
      ImportConfig(
        layoutsPath = NioPath.of(layoutsPath),
        outputPath = NioPath.of(outputPath),
        layoutName = layoutName.replace('_', ' '),
        scenarioId = scenarioId,
        nationIds = nationIds,
        allocationProfileCatalogPath = allocationProfileCatalogPath
      )

  private def parseLayouts(xmlText: String): Either[Throwable, Vector[LegacyLayout]] =
    Try {
      val root = XML.loadString(xmlText)
      (root \ "Layout").toVector.map { layoutNode =>
        val name = (layoutNode \ "Name").text.trim
        val xSize = (layoutNode \ "XSize").text.trim.toInt
        val ySize = (layoutNode \ "YSize").text.trim.toInt
        val spawns =
          (layoutNode \ "Spawn").toVector.map { spawnNode =>
            SpawnPoint(
              x = (spawnNode \ "X").text.trim.toInt,
              y = (spawnNode \ "Y").text.trim.toInt,
              spawnType = (spawnNode \ "SpawnType").text.trim.toUpperCase
            )
          }
        LegacyLayout(name, xSize, ySize, spawns)
      }
    }.toEither

  private def renderScenario(
      layout: LegacyLayout,
      config: ImportConfig
  ): Either[Throwable, String] =
    val playerSpawns = layout.spawns.filter(_.spawnType == "PLAYER")
    val throneSpawns = layout.spawns.filter(_.spawnType == "THRONE")

    if playerSpawns.size != config.nationIds.size then
      Left(
        IllegalArgumentException(
          s"Nation id count (${config.nationIds.size}) must match PLAYER spawn count (${playerSpawns.size})"
        )
      )
    else
      val players = playerSpawns.zip(config.nationIds).map { case (spawn, nationId) =>
        s"""
           |      {
           |        nation-id=$nationId
           |        profile-environment="surface"
           |        surface-start={ x=${spawn.x}, y=${spawn.y} }
           |      }
           |""".stripMargin.trim
      }

      val thrones = throneSpawns.map { spawn =>
        s"""
           |      {
           |        x=${spawn.x}
           |        y=${spawn.y}
           |        level=1
           |      }
           |""".stripMargin.trim
      }

      val wrapState = inferWrapState(layout.name)

      Right(
        s"""
           |scenarios=[
           |  {
           |    scenario-id="${config.scenarioId}"
           |    name="${layout.name}"
           |    dimensions={ x-size=${layout.xSize}, y-size=${layout.ySize} }
           |    wrap-state="$wrapState"
           |    layers={
           |      surface-enabled=true
           |      underground-enabled=false
           |      underground-plane-name="The Underworld"
           |      connect-every-province-with-tunnel=true
           |    }
           |    players=[
           |${players.mkString(",\n")}
           |    ]
           |    placements={
           |      surface-thrones=[
           |${thrones.mkString(",\n")}
           |      ]
           |      underground-thrones=[]
           |      gates=[]
           |    }
           |    allocation-profile-catalog-path="${config.allocationProfileCatalogPath}"
           |  }
           |]
           |""".stripMargin
      )

  private def inferWrapState(layoutName: String): String =
    val normalized = layoutName.toLowerCase
    if normalized.contains("torus") then "full"
    else if normalized.contains("horizontal wrap") then "horizontal"
    else if normalized.contains("vertical wrap") then "vertical"
    else "none"
