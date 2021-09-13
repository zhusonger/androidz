package cn.com.lasong.zapp.data.remote

import cn.com.lasong.utils.ILog
import cn.com.lasong.utils.ZCrypto
import cn.com.lasong.zapp.ZApp
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.tencent.mmkv.MMKV

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
    }

    // applicationContext is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    private val requestQueue: RequestQueue = Volley.newRequestQueue(ZApp.applicationContext())


    /**
     * 获取C/S交互密钥
     */
    fun validateClientKey(result: ((Int)->Unit)? = null) {
        val request = StringRequest("$BASE_URL/crypto/get_key",
            {
                MMKV.defaultMMKV()?.apply {
                    putString(ZApp.KEY_CLIENT_KEY_SIGNATURE, it).apply()
                }
                val ret = ZCrypto.decryptAndValidClient(it)
                ILog.d("validateClientKey : $ret")
                result?.invoke(ret)
            },
            {
                // 失败就用缓存
                MMKV.defaultMMKV()?.let {
                    return@let it.getString(ZApp.KEY_CLIENT_KEY_SIGNATURE, null)
                }.apply {
                    val ret = ZCrypto.decryptAndValidClient(this)
                    ILog.d("validateClientKey : $ret")
                    result?.invoke(ret)
                }
            }
        )
        requestQueue.add(request)
    }
}