package com.crib.bills.dom6maps
package services.mapeditor

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite
import cats.syntax.all.*
import model.ProvinceId
import model.TerrainFlag
import model.TerrainMask
import model.map.*

object GroundSurfaceDuelPipeSpec extends SimpleIOSuite:
  private def mapSizeDirective = MapSizePixels(MapWidthPixels(5 * 256), MapHeightPixels(5 * 160))

  test("pipe adds gates, thrones, and severs wrap"):
    val surface = Vector(
      mapSizeDirective,
      WrapAround,
      Gate(ProvinceId(1), ProvinceId(2)),
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(5), 0),
      Terrain(ProvinceId(21), 0),
      Terrain(ProvinceId(25), 0)
    )
    val cave = Vector(
      mapSizeDirective,
      WrapAround,
      Gate(ProvinceId(2), ProvinceId(3)),
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(5), 0),
      Terrain(ProvinceId(21), 0),
      Terrain(ProvinceId(25), 0)
    )
    import cats.instances.either.*
    type EC[A] = Either[Throwable, A]
    val pipe = new apps.services.mapeditor.GroundSurfaceDuelPipeImpl[IO](
      new apps.services.mapeditor.MapSizeValidatorImpl[IO],
      new apps.services.mapeditor.PlacementPlannerImpl[IO],
      new apps.services.mapeditor.GateDirectiveServiceImpl[IO],
      new apps.services.mapeditor.ThronePlacementServiceImpl[IO]
    )
    val res = pipe.apply[EC](Stream.emits(surface), Stream.emits(cave), GroundSurfaceDuelConfig.default)
    res.flatMap { ec =>
      IO.fromEither(ec.map { case (surfRes, caveRes) =>
        val gates = Vector(
          Gate(ProvinceId(3), ProvinceId(28)),
          Gate(ProvinceId(23), ProvinceId(48)),
          Gate(ProvinceId(11), ProvinceId(36)),
          Gate(ProvinceId(15), ProvinceId(40))
        )
        val thrones = Vector(1, 5, 21, 25).map(ProvinceId.apply)
        def hasThrones(ds: Vector[MapDirective]) =
          thrones.forall { id =>
            ds.exists {
              case Terrain(p, m) if p == id => TerrainMask(m).hasFlag(TerrainFlag.Throne)
              case _                         => false
            }
          }
        val surfChecks =
          gates.forall(surfRes.contains) &&
            hasThrones(surfRes) &&
            !surfRes.exists { case Gate(ProvinceId(1), ProvinceId(2)) => true; case _ => false } &&
            !surfRes.contains(WrapAround) && surfRes.contains(NoWrapAround)
        val caveChecks =
          gates.forall(caveRes.contains) &&
            hasThrones(caveRes) &&
            !caveRes.exists { case Gate(ProvinceId(2), ProvinceId(3)) => true; case _ => false } &&
            !caveRes.contains(WrapAround) && caveRes.contains(NoWrapAround)
        expect(surfChecks && caveChecks)
      })
    }
