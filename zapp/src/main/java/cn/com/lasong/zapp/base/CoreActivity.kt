package cn.com.lasong.zapp.base

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.CoreService

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/7
 * Description:
 * 需要使用核心服务的Activity
 */
open class CoreActivity : AppBaseActivity() {

    protected var mService: Messenger? = null
    private var bound = false
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = Messenger(service)
            bound = true

            // We want to monitor the service for as long as we are
            // connected to it.
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                val msg: Message = Message.obtain(null,
                        CoreService.MSG_REGISTER_CLIENT)
                msg.replyTo = mMessenger
                mService!!.send(msg)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Never Happen
            mService = null
            bound = false
        }
    }
    private val handler = Handler(Handler.Callback { msg ->
        return@Callback handleMessage(msg)
    })
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val mMessenger: Messenger = Messenger(handler)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, CoreService::class.java).also { intent->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //  先移除所有消息
        handler.removeCallbacksAndMessages(null)
        if (bound) {
            // 发送最后一条消息
            // unregister client
            if (mService != null) {
                try {
                    val msg: Message = Message.obtain(null,
                            CoreService.MSG_UNREGISTER_CLIENT)
                    msg.replyTo = mMessenger
                    mService!!.send(msg)
                } catch (e: RemoteException) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            unbindService(connection)
            bound = false
        }
    }

    /**   实际业务功能↓   **/
    open fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            CoreService.MSG_REGISTER_CLIENT -> ILog.d("MSG_REGISTER_CLIENT Response : " + msg.obj)
            CoreService.MSG_UNREGISTER_CLIENT -> ILog.d("MSG_UNREGISTER_CLIENT Response : " + msg.obj)
        }
        return true
    }

}