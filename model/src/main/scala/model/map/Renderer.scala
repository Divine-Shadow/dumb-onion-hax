package com.crib.bills.dom6maps
package model.map

trait Renderer[T]:
  def render(value: T): String

object Renderer:
  def apply[T](using r: Renderer[T]) = r

  extension [T <: MapDirective](value: T)(using r: Renderer[MapDirective])
    def render: String = r.render(value)

  given Renderer[MapDirective] with
    def render(directive: MapDirective): String =
        directive match
          case Dom2Title(value)      => s"#dom2title $value"
          case ImageFile(value)      => s"#imagefile $value"
          case WinterImageFile(value) => s"#winterimagefile $value"
          case MapSizePixels(w, h)    => s"#mapsize ${w.value} ${h.value}"
          case DomVersion(v)         => s"#domversion $v"
          case PlaneName(value)      => s"#planename $value"
          case Description(value)    => s"#description \"$value\""
          case WrapAround            => "#wraparound"
          case HWrapAround           => "#hwraparound"
          case VWrapAround           => "#vwraparound"
          case NoWrapAround          => "#nowraparound"
          case NoDeepCaves           => "#nodeepcaves"
          case NoDeepChoice          => "#nodeepchoice"
          case MapNoHide             => "#mapnohide"
          case MapTextColor(color)   =>
            def f(c: ColorComponent) = c.value.toString
            s"#maptextcol ${f(color.red)} ${f(color.green)} ${f(color.blue)} ${f(color.alpha)}"
          case MapDomColor(r,g,b,a)  => s"#mapdomcol $r $g $b $a"
          case SailDist(v)           => s"#saildist $v"
          case Features(v)           => s"#features $v" // added for completeness
          case AllowedPlayer(n)      => s"#allowedplayer ${n.id}"
          case SpecStart(n,p)        => s"#specstart ${n.id} ${p.value}"
          case Pb(x,y,l,p)           => s"#pb $x $y $l ${p.value}"
          case Terrain(p,m)          => s"#terrain ${p.value} $m"
          case LandName(p,n)         => s"#landname ${p.value} \"$n\""
          case SetLand(p)            => s"#setland ${p.value}"
          case Feature(f)            => s"#feature ${f.value}"
          case ProvinceFeature(p,f)  => s"#feature ${p.value} ${f.value}"
          case Gate(a,b)             => s"#gate ${a.value} ${b.value}"
          case Neighbour(a,b)        => s"#neighbour ${a.value} ${b.value}"
          case NeighbourSpec(a,b,f)  => s"#neighbourspec ${a.value} ${b.value} ${f.mask}"
          case Comment(value)        => s"--$value"
