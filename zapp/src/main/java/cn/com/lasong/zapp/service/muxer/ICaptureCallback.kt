package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/8/5
 * Description:
 */
interface ICaptureCallback {
    fun onMediaFormat(flag: Int, format: MediaFormat)
    fun onOutputBuffer(flag: Int, data: ByteBuffer, info: MediaCodec.BufferInfo)
}