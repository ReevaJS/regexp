package com.reeva.regexp

interface ExecutableRegExp {
    fun execute(source: String): MatchResult?
}

class MatchResult {

}

// class RegExp {
//     private val executable: ExecutableRegExp

//     fun matches(source: String): Boolean {
//         return executable.execute(source) != null
//     }

//     fun execute(source: String) = executable.execute(source)
// }
