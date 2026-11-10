package com.thelightphone.zork3.engine

internal class NoSuchKeyException : Exception {
    constructor() : super()
    constructor(s: String) : super(s)
}
