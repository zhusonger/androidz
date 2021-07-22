package cn.com.lasong.zapp.service.muxer

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import cn.com.lasong.zapp.data.RecordBean
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/22
 * Description:
 */
class Mpeg4Muxer : MediaCodec.Callback() {
    // 视频合成器
    private lateinit var muxer: MediaMuxer
    // 视频编码器
    private lateinit var videoEncoder: MediaCodec
    // 音频编码器
    private var audioEncoder: MediaCodec? = null
    // 录制文件路径
    private lateinit var path: String
    // 录制参数
    private lateinit var params: RecordBean
    // 编码器输入surface
    private lateinit var surface: Surface

    fun start(params: RecordBean) {
        this.params = params
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val fileName = simpleDateFormat.format(Calendar.getInstance().time)
        params.fileName = "Screen_$fileName.mp4"
        val file = File(params.saveDir!!, params.fileName!!)
        path = file.absolutePath
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 1.音频编码器
        if (params.audioEnable) {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                params.audioSampleRateValue,
                params.audioChannelCountValue
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, params.audioBitrateValue);
            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioEncoder?.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder?.start()
        }

        // 2.视频编码器
        val videoResolution = params.videoResolutionValue
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
            videoResolution.width, videoResolution.height)
        // MediaProjection 使用 surface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        // 有自己选择的码率就用, 否则使用视频对应的码率
        format.setInteger(
            MediaFormat.KEY_BIT_RATE,
            if (params.videoBitrate > 0) params.videoBitrateValue else videoResolution.bitrate
        )
        // 设置关键帧间隔1s
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.videoFpsValue)
        // 如果是自适应码率, 质量优先
        // 否则尽量接近设置的值
        format.setInteger(
            MediaFormat.KEY_BITRATE_MODE,
            if (params.videoBitrate == 0) MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            else MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
        )
        // 7.0以上设置KEY_PROFILE 才生效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
            format.setInteger(MediaFormat.KEY_LEVEL,
                MediaCodecInfo.CodecProfileLevel.AVCLevel41)
        }

        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.setCallback(this)
        videoEncoder.configure(format,null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = videoEncoder.createInputSurface()
        videoEncoder.start()
    }

    fun stop() {
        videoEncoder.setCallback(null)
        videoEncoder.stop()
        videoEncoder.release()

        audioEncoder?.stop()
        audioEncoder?.release()
        audioEncoder = null

        muxer.stop()
        muxer.release()
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        TODO("Not yet implemented")
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        TODO("Not yet implemented")
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        TODO("Not yet implemented")
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        TODO("Not yet implemented")
    }
}