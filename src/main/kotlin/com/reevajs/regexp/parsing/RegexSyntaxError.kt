package com.reevajs.regexp.parsing

import java.lang.Exception

class RegexSyntaxError(message: String, val offset: Int) : Exception(message)
