package com.reeva.regexp

import java.lang.Exception

class RegexSyntaxError(message: String, val offset: Int) : Exception(message)
