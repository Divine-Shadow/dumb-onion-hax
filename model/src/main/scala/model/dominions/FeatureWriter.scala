package com.crib.bills.dom6maps
package model.dominions

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.Stream

object FeatureWriter extends IOApp.Simple:
  private def sanitize(name: String): String =
    name
      .replaceAll("[^a-zA-Z0-9]+", " ")
      .split("\\s+")
      .filter(_.nonEmpty)
      .map(_.toLowerCase.capitalize)
      .mkString
  
  private def escape(str: String): String =
    str.replace("\\", "\\\\").replace("\"", "\\\"")

  def run: IO[Unit] =
    val input = Path("documentation/domain/dominions/data-dump/sites.txt")
    val output = Path("model/src/main/scala/model/dominions/Feature.scala")
    SiteParser.parseFile[IO](input).compile.toList.flatMap { sites =>
      val header =
        """|package com.crib.bills.dom6maps
           |package model.dominions
           |
           |final case class FeatureName(value: String) extends AnyVal
           |final case class FeatureId(value: Int) extends AnyVal
           |final case class FeatureRarity(value: Int) extends AnyVal
           |final case class FeaturePath(value: String) extends AnyVal
           |final case class IsThrone(value: Boolean) extends AnyVal
           |
           |enum Feature(
           |    val name: FeatureName,
           |    val id: FeatureId,
           |    val rarity: FeatureRarity,
           |    val path: FeaturePath,
           |    val throne: IsThrone
           |):
           |""".stripMargin

      val seen = scala.collection.mutable.Set.empty[String]
      val body = sites
        .map { site =>
          val base = sanitize(site.name.value)
          val name =
            if seen.add(base) then base else s"${base}${site.number.value}"
          s"  case $name extends Feature(FeatureName(\"${escape(site.name.value)}\"), FeatureId(${site.number.value}), FeatureRarity(${site.rarity.value}), FeaturePath(\"${escape(site.path.value)}\"), IsThrone(${site.throne.value}))"
        }
        .mkString("\n")

      val content = (header + body + "\n")
      Stream
        .emits(content.getBytes)
        .through(Files[IO].writeAll(output))
        .compile
        .drain
    }
