package com.crib.bills.dom6maps
package apps.services.mapeditor

import javax.swing.{JComboBox, JLabel, JPanel}
import model.Nation

final class GroundSurfaceNationPanel(defaultSurface: Nation, defaultUnderground: Nation) extends JPanel:
  private val surfaceBox = new JComboBox[Nation](Nation.values)
  private val undergroundBox = new JComboBox[Nation](Nation.values)
  surfaceBox.setSelectedItem(defaultSurface)
  undergroundBox.setSelectedItem(defaultUnderground)
  add(new JLabel("surface nation"))
  add(surfaceBox)
  add(new JLabel("underground nation"))
  add(undergroundBox)

  def surface: Nation = surfaceBox.getItemAt(surfaceBox.getSelectedIndex)
  def underground: Nation = undergroundBox.getItemAt(undergroundBox.getSelectedIndex)
