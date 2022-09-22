package com.reevajs.regexp.parser

import java.lang.Exception

class RegexSyntaxError(message: String, val offset: Int) : Exception(message)
