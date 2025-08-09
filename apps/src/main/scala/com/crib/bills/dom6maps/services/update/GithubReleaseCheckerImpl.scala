package com.crib.bills.dom6maps
package apps.services.update

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import cats.effect.Async
import model.version.{UpdateStatus, Version}
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class GithubReleaseCheckerImpl[Sequencer[_]: Async] extends Service[Sequencer]:

  override def checkForUpdate[ErrorChannel[_]](
      current: Version
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[UpdateStatus]] =
    val uri = URI("https://api.github.com/repos/Divine-Shadow/dumb-onion-hax/releases/latest")
    for
      client <- Async[Sequencer].delay(HttpClient.newHttpClient())
      request <- Async[Sequencer].delay(HttpRequest.newBuilder(uri).GET().build())
      responseEither <-
        Async[Sequencer]
          .blocking(client.send(request, HttpResponse.BodyHandlers.ofString()))
          .attempt
      result <- responseEither match
        case Right(response) =>
          val tagOpt = "\"tag_name\":\"(.*?)\"".r.findFirstMatchIn(response.body()).map(_.group(1))
          tagOpt match
            case Some(tag) =>
              val latest = Version(tag.stripPrefix("v"))
              val status =
                if latest.value == current.value then UpdateStatus.CurrentVersionIsLatest
                else UpdateStatus.UpdateAvailable
              status.pure[ErrorChannel].pure[Sequencer]
            case None =>
              errorChannel
                .raiseError[UpdateStatus](RuntimeException("tag_name not found in GitHub response"))
                .pure[Sequencer]
        case Left(err) =>
          errorChannel.raiseError[UpdateStatus](err).pure[Sequencer]
    yield result
