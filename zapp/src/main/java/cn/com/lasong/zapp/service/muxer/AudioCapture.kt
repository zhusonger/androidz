package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import cn.com.lasong.media.record.audio.AdapterAudioListener
import cn.com.lasong.media.record.audio.AudioRecorder
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 * 音频捕获类
 */
class AudioCapture(private val scope: CoroutineScope) {
    // 音频编码器
    private lateinit var audioEncoder: MediaCodec

    // 当前状态
    var state = Mpeg4Muxer.STATE_IDLE

    private lateinit var builder: AudioRecorder.Builder

    private val audioListener =  AdapterAudioListener()

    /**
     * 开启录音以及编码器
     */
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

        scope.launch {
            builder = AudioRecorder.Builder().apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                setSampleRateInHz(params.audioSampleRateValue)
                setChannelConfig(params.audioChannelValue)
                registerListener(audioListener)
            }
            if(!AudioRecorder.getInstance().start(builder)) {
                stop()
                return@launch
            }
            state = Mpeg4Muxer.STATE_START
        }
    }

    /**
     * 停止录音及编码器
     */
    fun stop() {
        state = Mpeg4Muxer.STATE_STOP
        AudioRecorder.getInstance().stop()
        try {
            audioEncoder.stop()
            audioEncoder.release()
        } catch (e: Exception) {
            ILog.e(RecordService.TAG, e)
        }
        state = Mpeg4Muxer.STATE_IDLE
    }
}