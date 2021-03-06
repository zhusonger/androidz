package cn.com.lasong.zapp

import android.app.Service
import android.content.Intent
import android.os.*


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/5
 * Description:
 * 核心服务
 */

class CoreService : Service(), Handler.Callback {

    companion object {
        /**
         * Command to the service to register a client, receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client where callbacks should be sent.
         */
        const val MSG_REGISTER_CLIENT = 1

        /**
         * Command to the service to unregister a client, ot stop receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client as previously given with MSG_REGISTER_CLIENT.
         */
        const val MSG_UNREGISTER_CLIENT = 2

        /**
         * 回复OK表示成功
         */
        const val RES_OK = "OK"
    }

    /** Keeps track of all current registered clients.  */
    var mClients = ArrayList<Messenger>()

    private val handler = Handler(this)
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val mMessenger: Messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    override fun onDestroy() {
        super.onDestroy()
        mClients.clear()
        handler.removeCallbacksAndMessages(null)
    }
    /**
     * 通知客户端
     */
    private fun notifyClients(msg: Message) {
        for (i in mClients.indices.reversed()) {
            try {
                mClients[i].send(msg)
            } catch (e: RemoteException) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.removeAt(i)
            }
        }
    }

    /**   实际业务功能↓   **/
    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_REGISTER_CLIENT -> {
                mClients.add(msg.replyTo)
                val replayMsg = Message.obtain(handler)
                replayMsg.what = MSG_REGISTER_CLIENT
                replayMsg.obj = RES_OK
                notifyClients(replayMsg)
            }
            MSG_UNREGISTER_CLIENT -> {
                val replayMsg = Message.obtain(handler)
                replayMsg.what = MSG_UNREGISTER_CLIENT
                replayMsg.obj = RES_OK
                notifyClients(replayMsg)
                mClients.remove(msg.replyTo)
            }
        }
        return true
    }


}