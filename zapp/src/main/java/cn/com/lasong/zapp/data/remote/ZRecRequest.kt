package cn.com.lasong.zapp.data.remote

import cn.com.lasong.utils.ILog
import cn.com.lasong.utils.ZCrypto
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import java.nio.charset.Charset


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/9/28
 * Description: 请求封装
 */
class ZRecRequest(
    method: Int = Method.GET,
    url: String?,
    requestBody: String? = null,
    listener: Response.Listener<String>? = null,
    errorListener: Response.ErrorListener? = null,
    private val encrypt: Boolean = false,
) : JsonRequest<String>(method, url, requestBody, listener, errorListener) {

    private val header: MutableMap<String, String> = mutableMapOf()

    init {
        runCatching {
            header["signature"] = ZCrypto.signature(requestBody ?: "")
        }
        header.putAll(NetManager.COMMON_HEADERS)
        ILog.d("request: $method, url: $url, requestBody: $requestBody")
    }

    override fun getHeaders(): MutableMap<String, String> {
        return header
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
        var parsed: String = runCatching {
            val charset = Charset.forName(HttpHeaderParser.parseCharset(response!!.headers))
            String(response.data, charset)
        }.getOrElse { return Response.error(VolleyError(response)) }
        ILog.d("parseNetworkResponse: $url, parsed: $parsed")
        // 解密
        if (encrypt) {
            parsed = runCatching {
                ZCrypto.decryptAES(parsed)
            }.getOrElse { return Response.error(VolleyError(response)) }
        }

        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
    }
}