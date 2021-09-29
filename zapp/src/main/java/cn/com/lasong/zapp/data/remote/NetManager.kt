package cn.com.lasong.zapp.data.remote

import cn.com.lasong.utils.SecretUtils
import cn.com.lasong.utils.ZCrypto
import cn.com.lasong.zapp.BuildConfig
import cn.com.lasong.zapp.ZApp
import cn.com.lasong.zapp.data.VersionBean
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/9/13
 * Description: 网络请求库
 */
class NetManager private constructor() {
    companion object {
        private const val BASE_URL = "http://192.168.120.131:5101"
        val INSTANCE = NetManager()
        val COMMON_HEADERS: Map<String, String> = mapOf(
            "version" to BuildConfig.VERSION_CODE.toString(),
            "package" to BuildConfig.APPLICATION_ID,
            "app_signature" to SecretUtils.getSignature(ZApp.applicationContext())
        )
    }

    // applicationContext is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    private val requestQueue: RequestQueue = Volley.newRequestQueue(ZApp.applicationContext())

    /**
     * 验证C/S交互密钥
     */
    suspend fun validateClientKey() = suspendCoroutine<Int> { coroutine ->
        val request = ZRecRequest(
            url = "$BASE_URL/crypto/get_key",
            listener = {
                MMKV.defaultMMKV()?.apply {
                    putString(ZApp.KEY_CLIENT_KEY_SIGNATURE, it).apply()
                }
                val ret = runCatching {
                    ZCrypto.decryptAndValidClient(it)
                }.getOrDefault(-1)
                coroutine.resume(ret)
            },
            errorListener = {
                // 失败就用缓存
                MMKV.defaultMMKV()?.let {
                    return@let it.getString(ZApp.KEY_CLIENT_KEY_SIGNATURE, null)
                }.apply {
                    val ret = runCatching {
                        ZCrypto.decryptAndValidClient(this)
                    }.getOrDefault(-1)
                    coroutine.resume(ret)
                }
            },
            encrypt = false
        )
        requestQueue.add(request)
    }

    // 检测版本
    suspend fun checkVersions() = suspendCoroutine<List<VersionBean>> { coroutine ->
        val request = ZRecRequest(url = "$BASE_URL/about/versions",
            listener = {
                val versions: List<VersionBean> = ZApp.JSON.fromJson(
                    it,
                    object : TypeToken<List<VersionBean>>() {}.type
                )
                coroutine.resume(versions)
            }, errorListener = {
                coroutine.resumeWithException(it)
            })
        requestQueue.add(request)
    }
}