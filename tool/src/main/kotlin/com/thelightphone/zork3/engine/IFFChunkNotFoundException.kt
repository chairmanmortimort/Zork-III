package com.thelightphone.zork3.engine

/** Thrown when an expected IFF chunk is missing (ported from Zplet). */
class IFFChunkNotFoundException : Exception {
    constructor() : super()
    constructor(s: String) : super(s)
}
