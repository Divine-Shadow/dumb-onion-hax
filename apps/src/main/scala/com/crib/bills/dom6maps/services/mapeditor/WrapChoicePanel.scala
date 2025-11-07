package com.crib.bills.dom6maps
package apps.services.mapeditor

import javax.swing.{BoxLayout, ButtonGroup, JPanel, JRadioButton}
import java.awt.event.ActionListener

final class WrapChoicePanel(default: WrapChoice, allowGroundSurfaceDuel: Boolean = false) extends JPanel:
  setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
  private val h = new JRadioButton("hwrap")
  private val v = new JRadioButton("vwrap")
  private val f = new JRadioButton("full-wrap")
  private val n = new JRadioButton("no-wrap")
  private val duel = new JRadioButton("ground-surface duel")
  private val group = new ButtonGroup()
  group.add(h); group.add(v); group.add(f); group.add(n)
  if allowGroundSurfaceDuel then group.add(duel)
  default match
    case WrapChoice.HWrap            => h.setSelected(true)
    case WrapChoice.VWrap            => v.setSelected(true)
    case WrapChoice.FullWrap         => f.setSelected(true)
    case WrapChoice.NoWrap           => n.setSelected(true)
    case WrapChoice.GroundSurfaceDuel => duel.setSelected(true)
  add(h); add(v); add(f); add(n); if allowGroundSurfaceDuel then add(duel)

  def choice: WrapChoice =
    if h.isSelected then WrapChoice.HWrap
    else if v.isSelected then WrapChoice.VWrap
    else if f.isSelected then WrapChoice.FullWrap
    else if allowGroundSurfaceDuel && duel.isSelected then WrapChoice.GroundSurfaceDuel
    else WrapChoice.NoWrap

  def setEnabledAll(enabled: Boolean): Unit =
    h.setEnabled(enabled)
    v.setEnabled(enabled)
    f.setEnabled(enabled)
    n.setEnabled(enabled)
    if allowGroundSurfaceDuel then duel.setEnabled(enabled)

  def onChange(callback: => Unit): Unit =
    val l = new ActionListener:
      override def actionPerformed(e: java.awt.event.ActionEvent): Unit = callback
    h.addActionListener(l)
    v.addActionListener(l)
    f.addActionListener(l)
    n.addActionListener(l)
    if allowGroundSurfaceDuel then duel.addActionListener(l)
