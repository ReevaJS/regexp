package com.reevajs.regexp.parser

import java.lang.Exception

class RegExpSyntaxError(message: String, val offset: Int) : Exception(message)
