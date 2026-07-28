package com.lonnnnnng.biu.data.bilibili

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class SignedWbiQuery(
    val parameters: Map<String, String>,
    val encodedQuery: String,
)

object WbiSigner {
    private val mixinKeyEncTable = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
    )
    private val filteredCharacters = Regex("[!'()*]")

    fun sign(
        parameters: Map<String, Any?>,
        imgKey: String,
        subKey: String,
        timestampSeconds: Long,
    ): SignedWbiQuery {
        require(imgKey.isNotBlank() && subKey.isNotBlank()) { "WBI keys must not be blank" }

        val normalized = buildMap {
            parameters.forEach { (key, value) ->
                if (value != null) put(key, value.toString())
            }
            put("wts", timestampSeconds.toString())
        }
        val encodedParameters = normalized.toSortedMap().entries.joinToString("&") { (key, value) ->
            val filteredValue = value.replace(filteredCharacters, "")
            "${percentEncode(key)}=${percentEncode(filteredValue)}"
        }
        val mixinKey = getMixinKey(imgKey + subKey)
        val signature = md5(encodedParameters + mixinKey)

        return SignedWbiQuery(
            parameters = normalized + ("w_rid" to signature),
            encodedQuery = encodedParameters + "&w_rid=$signature",
        )
    }

    private fun getMixinKey(source: String): String {
        require(source.length >= 64) { "Combined WBI key must contain at least 64 characters" }
        return mixinKeyEncTable
            .asSequence()
            .map(source::get)
            .joinToString(separator = "")
            .take(32)
    }

    private fun percentEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
            .replace("%7E", "~")
    }

    private fun md5(value: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
