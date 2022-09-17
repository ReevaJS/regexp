package com.reevajs.regexp

import java.lang.Exception

class RegexSyntaxError(message: String, val offset: Int) : Exception(message)
