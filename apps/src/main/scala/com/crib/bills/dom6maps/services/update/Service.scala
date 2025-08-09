package com.crib.bills.dom6maps
package apps.services.update

import cats.{MonadError, Traverse}
import model.version.{UpdateStatus, Version}

trait Service[Sequencer[_]]:
  protected def updateService: Service[Sequencer] = this

  def checkForUpdate[ErrorChannel[_]](
      current: Version
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[UpdateStatus]]
