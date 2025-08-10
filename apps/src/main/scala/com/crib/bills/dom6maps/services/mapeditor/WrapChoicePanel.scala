package com.crib.bills.dom6maps
package apps.services.mapeditor

import javax.swing.{ButtonGroup, JPanel, JRadioButton}

final class WrapChoicePanel(default: WrapChoice) extends JPanel:
  private val h = new JRadioButton("hwrap")
  private val v = new JRadioButton("vwrap")
  private val n = new JRadioButton("no-wrap")
  private val group = new ButtonGroup()
  group.add(h); group.add(v); group.add(n)
  default match
    case WrapChoice.HWrap => h.setSelected(true)
    case WrapChoice.VWrap => v.setSelected(true)
    case WrapChoice.NoWrap => n.setSelected(true)
  add(h); add(v); add(n)

  def choice: WrapChoice =
    if h.isSelected then WrapChoice.HWrap
    else if v.isSelected then WrapChoice.VWrap
    else WrapChoice.NoWrap

  def setEnabledAll(enabled: Boolean): Unit =
    h.setEnabled(enabled)
    v.setEnabled(enabled)
    n.setEnabled(enabled)
