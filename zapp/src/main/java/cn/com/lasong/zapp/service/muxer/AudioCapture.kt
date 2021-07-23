package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import cn.com.lasong.zapp.data.RecordBean

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * 音频捕获类
 */
class AudioCapture {
    // 音频编码器
    private lateinit var audioEncoder: MediaCodec

    // 当前状态
    var state = Mpeg4Muxer.STATE_IDLE

    fun start(params: RecordBean) {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            params.audioSampleRateValue,
            params.audioChannelCountValue
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, params.audioBitrateValue);
        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder.start()
        state = Mpeg4Muxer.STATE_START
    }

    fun stop() {
        audioEncoder.stop()
        audioEncoder.release()
        state = Mpeg4Muxer.STATE_STOP
    }
}