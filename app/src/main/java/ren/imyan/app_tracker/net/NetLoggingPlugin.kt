package ren.imyan.app_tracker.net

import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import timber.log.Timber

val NetLoggingPlugin = createClientPlugin("NetLoggingPlugin") {


    onRequest { request, _ ->
        Timber.d(
            "\n\t" +
                    """
            
            |────── Request ────────────────────────────────────────────────────────────────────────>
            | Method: @${request.method.value}
            |
            | Url: ${request.url}
            |
            | Headers----------------->
            | ${parseHeaders(request.headers.build())}
            | <-----------------Headers
            |
            | Body----------------->
            | ${JsonUtils.format(request.body.toString())}
            | <-----------------Body
            |
            |────── Request ────────────────────────────────────────────────────────────────────────>
        """.trimMargin()
        )
    }

    onResponse { response ->
        Timber.d(
            """
            
            |<────── Response ────────────────────────────────────────────────────────────────────────
            | Status: ${response.status}
            |
            | Url: ${response.request.url}
            |
            | Headers----------------->
            | ${parseHeaders(response.headers)}
            | <-----------------Headers
            |
            | Body----------------->
            | ${JsonUtils.format(response.body())}
            | <-----------------Body
            |
            | Time: ${1}ms
            |
            |<────── Response ────────────────────────────────────────────────────────────────────────
        """.trimMargin()
        )
    }
}

private fun parseHeaders(headers: Headers): String {
    val headersStr = StringBuilder()
    headers.entries().forEach { (key, value) ->
        headersStr.append("$key: $value\n")
    }
    return headersStr.toString()
}

private object JsonUtils {

    fun format(json: String): String =
        if ((json.startsWith("{") && json.endsWith("}"))
            || (json.startsWith("[") && json.endsWith("]"))
        ) {
            formatJson(decodeUnicode(json))
        } else {
            json
        }

    /**
     * 格式化json字符串
     *
     * @param jsonStr 需要格式化的json串
     * @return 格式化后的json串
     */
    private fun formatJson(jsonStr: String?): String {
        if (null == jsonStr || "" == jsonStr) return ""
        val sb = StringBuilder()
        var last = '\u0000'
        var current = '\u0000'
        var indent = 0
        for (element in jsonStr) {
            last = current
            current = element
            //遇到{ [换行，且下一行缩进
            when (current) {
                '{', '[' -> {
                    sb.append(current)
                    sb.append('\n')
                    indent++
                    addIndentBlank(sb, indent)
                }
                //遇到} ]换行，当前行缩进
                '}', ']' -> {
                    sb.append('\n')
                    indent--
                    addIndentBlank(sb, indent)
                    sb.append(current)
                }
                //遇到,换行
                ',' -> {
                    sb.append(current)
                    if (last != '\\') {
                        sb.append('\n')
                        addIndentBlank(sb, indent)
                    }
                }
                else -> sb.append(current)
            }
        }
        return sb.toString()
    }

    /**
     * 添加space
     *
     * @param sb
     * @param indent
     */
    private fun addIndentBlank(sb: StringBuilder, indent: Int) {
        for (i in 0 until indent) {
            sb.append('\t')
        }
    }

    /**
     * http 请求数据返回 json 中中文字符为 unicode 编码转汉字转码
     *
     * @param theString
     * @return 转化后的结果.
     */
    private fun decodeUnicode(theString: String): String {
        var aChar: Char
        val len = theString.length
        val outBuffer = StringBuffer(len)
        var x = 0
        while (x < len) {
            aChar = theString[x++]
            if (aChar == '\\') {
                aChar = theString[x++]
                if (aChar == 'u') {
                    var value = 0
                    for (i in 0..3) {
                        aChar = theString[x++]
                        value = when (aChar) {
                            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (value shl 4) + aChar.code - '0'.code
                            'a', 'b', 'c', 'd', 'e', 'f' -> (value shl 4) + 10 + aChar.code - 'a'.code
                            'A', 'B', 'C', 'D', 'E', 'F' -> (value shl 4) + 10 + aChar.code - 'A'.code
                            else -> throw IllegalArgumentException(
                                "Malformed   \\uxxxx   encoding."
                            )
                        }

                    }
                    outBuffer.append(value.toChar())
                } else {
                    when (aChar) {
                        't' -> aChar = '\t'
                        'r' -> aChar = '\r'
                        'n' -> aChar = '\n'
                        'f' -> aChar = '\u000C'
                    }
//                        aChar = '\f'
                    outBuffer.append(aChar)
                }
            } else
                outBuffer.append(aChar)
        }
        return outBuffer.toString()
    }
}