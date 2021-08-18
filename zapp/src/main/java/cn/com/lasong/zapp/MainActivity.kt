package cn.com.lasong.zapp

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Message
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.base.CoreActivity
import cn.com.lasong.zapp.base.contract.MediaProjectRequest
import cn.com.lasong.zapp.data.RecordState
import cn.com.lasong.zapp.database.VideoEntity
import cn.com.lasong.zapp.databinding.ActivityMainBinding
import cn.com.lasong.zapp.databinding.ViewDelayBinding
import cn.com.lasong.zapp.service.CoreService
import cn.com.lasong.zapp.service.RecordService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : CoreActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var mCaptureLauncher: ActivityResultLauncher<Void>
    private lateinit var viewModel: MainViewModel

    private var delayJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, RecordService::class.java).also { intent->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_video, R.id.nav_about), binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        viewModel.targetState.observe(this, {
            when (it) {
                // 1.1! 查询是否正在录制
                RecordState.READY -> {
                    viewModel.updateCurrent(RecordState.READY)
                    sendMessage(Message.obtain(handler, RecordService.MSG_QUERY_RECORD))
                }
                // 1.3! 开始录制
                RecordState.START -> {
                    delayJob = lifecycleScope.launch {
                        val params = viewModel.params.value
                        val delay = params?.delayValue!!
                        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                        val binding = ViewDelayBinding.inflate(layoutInflater)
                        windowManager.addView(binding.root, WindowManager.LayoutParams().also { lp->
                            lp.format = PixelFormat.RGBA_8888
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        })

                        val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.record_delay_anim)
                        for (sec in delay downTo 1) {
                            binding.tvDelay.text = sec.toString()
                            binding.tvDelay.startAnimation(anim)
                            delay(1000)
                        }
                        windowManager.removeView(binding.root)
                        viewModel.updateCurrent(RecordState.START)
                        val message = Message.obtain(handler, RecordService.MSG_RECORD_START)
                        val data = viewModel.captureResult.value ?: Intent()
                        data.putExtra(RecordService.KEY_RECORD_PARAMS, viewModel.params.value)
                        message.obj = data
                        sendMessage(message)
                    }
                }
                // 1.4! 启动完成, 更新UI为运行中
                RecordState.RUNNING -> {
                    viewModel.updateCurrent(RecordState.RUNNING)
                    // 把服务设置成跟activity无关
                    startService(Intent(this, RecordService::class.java))
                }
                // 1.5! 停止录制
                RecordState.STOP -> {
                    delayJob?.cancel()
                    viewModel.updateCurrent(RecordState.STOP)
                    val message = Message.obtain(handler, RecordService.MSG_RECORD_STOP)
                    sendMessage(message)

                }
                // 1.6! 默认状态
                RecordState.IDLE -> {
                    // 在service里已经stop掉了, 不需要再次调用stopService
                    viewModel.updateCurrent(RecordState.IDLE)
                }
                // do nothing
                else -> {
                    ILog.d("targetState : $it")
                }
            }
        })
        mCaptureLauncher = registerForActivityResult(MediaProjectRequest()) {
            // 1.2.1.1 没有给权限, 忽略并重置状态
            if (null == it) {
                viewModel.updateTarget(RecordState.IDLE)
                viewModel.updateCurrent(RecordState.IDLE)
                return@registerForActivityResult
            }
            // 1.2.1.2 更新结果
            viewModel.updateCapture(it)
            viewModel.updateTarget(RecordState.START)
        }
    }

    override fun onStart() {
        super.onStart()
        sendMessage(Message.obtain(handler, RecordService.MSG_QUERY_VIDEO))
    }

    override fun onStop() {
        super.onStop()
        sendMessage(Message.obtain(handler, RecordService.MSG_UPDATE_SCREEN_SHOT))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            // 进入时查询当前状态
            CoreService.MSG_REGISTER_CLIENT -> {
                val message = Message.obtain(handler, RecordService.MSG_QUERY_LAST_RECORD)
                sendMessage(message)
            }
            // 返回当前状态, 如果正在录制, 就同步下状态
            RecordService.MSG_QUERY_LAST_RECORD -> {
                val map = msg.obj as Map<*, *>
                val recording  = map[RecordService.KEY_RECORDING] as Boolean
                val startTime = map[RecordService.KEY_RECORD_START_TIME] as Long
                if (recording) {
                    viewModel.updateTarget(RecordState.RUNNING)
                    viewModel.startTimer(startTime)
                }
            }
            // 1.2! 查询到当前状态
            RecordService.MSG_QUERY_RECORD -> {
                val map = msg.obj as Map<*, *>
                val recording  = map[RecordService.KEY_RECORDING] as Boolean
                val mediaDataExist = map[RecordService.KEY_MEDIA_DATA_EXIST] as Boolean
                val startTime = map[RecordService.KEY_RECORD_START_TIME] as Long
                // 1.2.1 如果不在录制且没有录制对象, 请求录制权限
                if (!recording && !mediaDataExist) {
                    mCaptureLauncher.launch(null)
                }
                // 1.2.2 如果不在录制且有录制对象, 直接开始录制
                else if (!recording && mediaDataExist) {
                    viewModel.updateTarget(RecordState.START)
                }
                // 1.2.3 正在录制, 同步下状态即可
                else if (recording) {
                    viewModel.updateTarget(RecordState.RUNNING)
                    viewModel.startTimer(startTime)
                }
            }
            // 1.4! 启动完成, 更新为运行中
            RecordService.MSG_RECORD_START -> {
                val result = msg.obj as String
                if (result == CoreService.RES_OK) {
                    val data = msg.data
                    val startTime = data.getLong(RecordService.KEY_RECORD_START_TIME)
                    viewModel.updateTarget(RecordState.RUNNING)
                    viewModel.startTimer(startTime)
                    moveTaskToBack(true)
                }
            }
            // 1.5! 已经停止完成, 设置为闲置状态
            RecordService.MSG_RECORD_STOP -> {
                val result = msg.obj as String
                if (result == CoreService.RES_OK) {
                    viewModel.updateTarget(RecordState.IDLE)
                    viewModel.stopTimer()
                }
            }
            // 查询结果
            RecordService.MSG_QUERY_VIDEO -> {
                val result = msg.obj as Bundle
                val video = result.getParcelable<VideoEntity?>(RecordService.KEY_VIDEO)
                val recording = result.getBoolean(RecordService.KEY_RECORDING)
                viewModel.updateRecording(video, recording)
            }

        }
        return super.handleMessage(msg)
    }
}