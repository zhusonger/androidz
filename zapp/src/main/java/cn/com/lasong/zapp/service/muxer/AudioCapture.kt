package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import cn.com.lasong.media.record.audio.AdapterAudioListener
import cn.com.lasong.media.record.audio.AudioRecorder
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.data.RecordBean
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.*
import android.media.AudioRecord
import java.nio.ByteBuffer


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
    private var state = Mpeg4Muxer.STATE_IDLE

    private lateinit var builder: AudioRecorder.Builder

    private val audioListener =  AdapterAudioListener()

    // 每帧时长, 微妙
    private var spanTimeUs = 0L
    private var ptsUs = 0L
    private var frameJob: Job? = null
    /**
     * 开启录音以及编码器
     */
    fun start(params: RecordBean) {
        if (state != Mpeg4Muxer.STATE_IDLE) {
            return
        }
        state = Mpeg4Muxer.STATE_READY
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            params.audioSampleRateValue,
            params.audioChannelCountValue
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, params.audioBitrateValue);
        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder.start()

        frameJob = scope.async(start = CoroutineStart.LAZY) {
            try {
                while (isActive) {
                    if (state == Mpeg4Muxer.STATE_STOP) {
                        break
                    }
                    doFrame()
                    delay(spanTimeUs/1000)
                }
            } catch (e: Exception) {
                ILog.e(RecordService.TAG, e)
            }
        }
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
            spanTimeUs = params.audioSpanTimeUs
            state = Mpeg4Muxer.STATE_START
            // 开启读取麦克风数据并编码的任务
            frameJob?.start()
        }
    }

    /**
     * 停止录音及编码器
     */
    suspend fun stop() {
        if (state == Mpeg4Muxer.STATE_STOP || state == Mpeg4Muxer.STATE_IDLE) {
            return
        }
        state = Mpeg4Muxer.STATE_STOP
        AudioRecorder.getInstance().stop()
        frameJob?.cancel("STOP")
        doFrame(true)
        runCatching {
            audioEncoder.stop()
            audioEncoder.release()
        }
        state = Mpeg4Muxer.STATE_IDLE
    }

    /**
     * 读取AudioRecord写入编码器 读取编码器数据
     */
    private suspend fun doFrame(endOfStream: Boolean = false) {
        withContext(Dispatchers.IO) {
            runCatching {
                val bufferIndex: Int = audioEncoder.dequeueInputBuffer(0)
                if (bufferIndex >= 0) {
                    // 1. 录音数据写入编码器
                    val buffer = audioEncoder.getInputBuffer(bufferIndex)
                    val audioSize: Int = AudioRecorder.getInstance().read(buffer, buffer?.remaining()!!)

                    if (audioSize == AudioRecord.ERROR_INVALID_OPERATION
                        || audioSize == AudioRecord.ERROR_BAD_VALUE) {
                        ILog.e(RecordService.TAG, "Audio read error $audioSize")
                    } else {
                        val flag = if (endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        audioEncoder.queueInputBuffer(bufferIndex, 0, audioSize, ptsUs, flag)
                        ptsUs += spanTimeUs
                    }

                    // 2. 编码器编码并获取数据
                    while (isActive) {
                        if (state == Mpeg4Muxer.STATE_IDLE || !doDrainFrame(endOfStream)) {
                            break
                        }
                    }
                } else {
                    ILog.e(RecordService.TAG, "Audio dequeueInputBuffer bufferIndex $bufferIndex")
                }
            }
        }
    }

    /**
     * 获取编码后的帧数据
     */
    private fun doDrainFrame(endOfStream: Boolean = false): Boolean {
        val info = MediaCodec.BufferInfo()
        val bufferIndex: Int = audioEncoder.dequeueOutputBuffer(info, 0)
        if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            val format: MediaFormat = audioEncoder.outputFormat
            ILog.d(RecordService.TAG, "Audio format : $format")
        } else if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            ILog.d(RecordService.TAG, "Audio INFO_TRY_AGAIN_LATER")
            // 等待结束标志读取结束
            if (endOfStream) {
                return true
            }
            return false
        } else if (bufferIndex < 0) {
            ILog.d(RecordService.TAG, "Audio bufferIndex < 0")
        } else if (bufferIndex >= 0) {
            val data: ByteBuffer? = audioEncoder.getOutputBuffer(bufferIndex)
            if (null != data) {
                data.position(info.offset)
                data.limit(info.offset + info.size)
                info.offset = 0
            }
            val encoderPtsNs = info.presentationTimeUs * 1000
            ILog.d(RecordService.TAG, "Audio getOutputBuffer $bufferIndex : len = ${data?.limit()}," +
                    " encoderPtsNs = $encoderPtsNs")

            audioEncoder.releaseOutputBuffer(bufferIndex, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                ILog.d(RecordService.TAG, "Audio BUFFER_FLAG_END_OF_STREAM")
                return false
            }
        }
        return true
    }
}