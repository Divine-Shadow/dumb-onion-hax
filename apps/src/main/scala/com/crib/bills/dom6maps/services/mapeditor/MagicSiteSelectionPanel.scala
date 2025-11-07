package com.crib.bills.dom6maps
package apps.services.mapeditor

import java.awt.Component
import javax.swing.{BorderFactory, Box, BoxLayout, JCheckBox, JLabel, JPanel}

final class MagicSiteSelectionPanel(
    availability: CaveLayerAvailability
) extends JPanel:
  setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
  setBorder(BorderFactory.createTitledBorder("High-magic sites"))
  private val surfaceBox = new JCheckBox("Apply to overworld")
  private val caveBox    = new JCheckBox("Apply to cave layer")
  private val description = new JLabel("Choose which layers receive scripted sites:")

  surfaceBox.setSelected(false)
  caveBox.setSelected(false)
  caveBox.setEnabled(availability.isAvailable)
  if !availability.isAvailable then
    caveBox.setToolTipText("Cave layer not detected")

  Seq(description, surfaceBox, caveBox).foreach(_.setAlignmentX(Component.LEFT_ALIGNMENT))
  add(description)
  add(Box.createVerticalStrut(4))
  add(surfaceBox)
  add(caveBox)

  def selection: MagicSiteSelection =
    MagicSiteSelection(
      if surfaceBox.isSelected then MagicSiteToggle.Enabled else MagicSiteToggle.Disabled,
      if caveBox.isSelected then MagicSiteToggle.Enabled else MagicSiteToggle.Disabled
    )
