package com.thelightphone.zork3.engine

/** A run of text sharing one style, for the screen window (Zplet port). */
class StyleRegion {
    var style: Int = ZWindow.ROMAN
    var start: Int = 0
    var end: Int = 0
    var next: StyleRegion? = null
}
