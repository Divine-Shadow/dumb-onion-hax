package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import model.{Nation, ProvinceId}
import model.map.{AllowedPlayer, MapState, PlayerSpawn, SpecStart}

object SpawnPlacementServiceSpec extends SimpleIOSuite:
  test("update adds allowed players and start positions") {
    val service = new SpawnPlacementServiceImpl[IO]
    val spawns = Vector(
      PlayerSpawn(Nation.Agartha_Early, ProvinceId(1)),
      PlayerSpawn(Nation.Ulm_Early, ProvinceId(2))
    )
    for
      res <- service.update(MapState.empty, spawns)
      expectedPlayers = spawns.map(s => AllowedPlayer(s.nation))
      expectedStarts = spawns.map(s => SpecStart(s.nation, s.province))
    yield expect(res.allowedPlayers == expectedPlayers && res.startingPositions == expectedStarts)
  }
