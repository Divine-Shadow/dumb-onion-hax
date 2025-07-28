package com.crib.bills.dom6maps
package apps.services.map

import model.map.MapUploadRequest

trait Service[Sequencer[_]]:
  protected def mapUploadService: Service[Sequencer] = this

  def processUpload(request: MapUploadRequest): Sequencer[String]
