package com.crib.bills.dom6maps
package apps.services.mapeditor

import javax.swing.{ButtonGroup, JPanel, JRadioButton}
import java.awt.event.ActionListener

final class WrapChoicePanel(default: WrapChoice, allowGroundSurfaceDuel: Boolean = false) extends JPanel:
  private val h = new JRadioButton("hwrap")
  private val v = new JRadioButton("vwrap")
  private val n = new JRadioButton("no-wrap")
  private val duel = new JRadioButton("ground-surface duel")
  private val group = new ButtonGroup()
  group.add(h); group.add(v); group.add(n)
  if allowGroundSurfaceDuel then group.add(duel)
  default match
    case WrapChoice.HWrap           => h.setSelected(true)
    case WrapChoice.VWrap           => v.setSelected(true)
    case WrapChoice.NoWrap          => n.setSelected(true)
    case WrapChoice.GroundSurfaceDuel => duel.setSelected(true)
  add(h); add(v); add(n); if allowGroundSurfaceDuel then add(duel)

  def choice: WrapChoice =
    if h.isSelected then WrapChoice.HWrap
    else if v.isSelected then WrapChoice.VWrap
    else if allowGroundSurfaceDuel && duel.isSelected then WrapChoice.GroundSurfaceDuel
    else WrapChoice.NoWrap

  def setEnabledAll(enabled: Boolean): Unit =
    h.setEnabled(enabled)
    v.setEnabled(enabled)
    n.setEnabled(enabled)
    if allowGroundSurfaceDuel then duel.setEnabled(enabled)

  def onChange(f: => Unit): Unit =
    val l = new ActionListener:
      override def actionPerformed(e: java.awt.event.ActionEvent): Unit = f
    h.addActionListener(l)
    v.addActionListener(l)
    n.addActionListener(l)
    if allowGroundSurfaceDuel then duel.addActionListener(l)
