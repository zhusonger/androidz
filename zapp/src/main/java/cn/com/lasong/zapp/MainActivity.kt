package cn.com.lasong.zapp

import android.content.Intent
import android.os.Bundle
import android.os.Message
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cn.com.lasong.zapp.base.CoreActivity
import cn.com.lasong.zapp.base.contract.MediaProjectRequest
import cn.com.lasong.zapp.data.RecordState
import cn.com.lasong.zapp.databinding.ActivityMainBinding
import cn.com.lasong.zapp.service.CoreService
import cn.com.lasong.zapp.service.RecordService


class MainActivity : CoreActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var mCaptureLauncher: ActivityResultLauncher<Void>
    private lateinit var viewModel: MainViewModel

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
                R.id.nav_home, R.id.nav_gallery, R.id.nav_video), binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        viewModel.targetState.observe(this, {
            when (it) {
                // 1.1! 查询是否正在录制
                RecordState.READY -> {
                    viewModel.currentState.value = RecordState.READY
                    sendMessage(Message.obtain(handler, RecordService.MSG_QUERY_RECORD))
                }
                // 1.3! 开始录制
                RecordState.START -> {
                    viewModel.currentState.value = RecordState.START
                    val message = Message.obtain(handler, RecordService.MSG_RECORD_START)
                    val data = viewModel.captureResult.value ?: Intent()
                    data.putExtra(RecordService.KEY_RECORD_PARAMS, viewModel.params.value)
                    message.obj = data
                    sendMessage(message)
                }
                // 1.4 启动完成, 更新UI为运行中
                RecordState.RUNNING -> {
                    viewModel.currentState.value = RecordState.RUNNING
                    // 把服务设置成跟activity无关
                    startService(Intent(this, RecordService::class.java))
                }
                RecordState.STOP -> {
                    viewModel.currentState.value = RecordState.STOP
                    // 1.5
                }
            }
        })
        mCaptureLauncher = registerForActivityResult(MediaProjectRequest()) {
            // 1.2.1.1 没有给权限, 忽略并重置状态
            if (null == it) {
                viewModel.targetState.value = RecordState.IDLE
                viewModel.currentState.value = RecordState.IDLE
                return@registerForActivityResult
            }
            // 1.2.1.2 更新结果
            viewModel.captureResult.value = it
            viewModel.targetState.value = RecordState.START
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            // 进入时查询当前状态
            CoreService.MSG_REGISTER_CLIENT -> {
                val message = Message.obtain(handler, RecordService.MSG_PRE_RECORD)
                sendMessage(message)
            }
            // 返回当前状态, 如果正在录制, 就同步下状态
            RecordService.MSG_PRE_RECORD -> {
                val map = msg.obj as Map<*, *>
                val recording  = map[RecordService.KEY_RECORDING] as Boolean
                val startTime = map[RecordService.KEY_RECORD_START_TIME] as Long
                if (recording) {
                    viewModel.targetState.value = RecordState.RUNNING
                    viewModel.startTimer(startTime)
                }
            }
            // 1.2! 查询到当前状态
            RecordService.MSG_QUERY_RECORD -> {
                val map = msg.obj as Map<*, *>
                val recording  = map[RecordService.KEY_RECORDING] as Boolean
                val mediaProjectionNull = map[RecordService.KEY_MEDIA_PROJECTION_NULL] as Boolean
                val startTime = map[RecordService.KEY_RECORD_START_TIME] as Long
                // 1.2.1 如果不在录制且没有录制对象, 请求录制权限
                if (!recording && mediaProjectionNull) {
                    mCaptureLauncher.launch(null)
                }
                // 1.2.2 如果不在录制且有录制对象, 直接开始录制
                else if (!recording && !mediaProjectionNull) {
                    viewModel.targetState.value = RecordState.START
                }
                // 1.2.3 正在录制, 同步下状态即可
                else if (recording) {
                    viewModel.targetState.value = RecordState.RUNNING
                    viewModel.startTimer(startTime)
                }
            }
            // 1.4! 启动完成, 更新为运行中
            RecordService.MSG_RECORD_START -> {
                val result = msg.obj as String
                if (result == CoreService.RES_OK) {
                    val data = msg.data
                    val startTime = data.getLong(RecordService.KEY_RECORD_START_TIME)
                    viewModel.targetState.value = RecordState.RUNNING
                    viewModel.startTimer(startTime)
                }
            }
        }
        return super.handleMessage(msg)
    }
}