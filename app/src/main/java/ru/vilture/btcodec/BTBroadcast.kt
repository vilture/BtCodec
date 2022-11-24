package ru.vilture.btcodec

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BTBroadcast private constructor(callback: Callback) : BroadcastReceiver() {

    private val btCallback: Callback?

    init {
        btCallback = callback
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BR", "Зашли в BroadcastReceiver c $intent.action")
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            onBluetoothConnected()
        }
        if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            onBluetoothDisconnected()
        }
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothState()
        }
    }

    private fun onBluetoothState() {
        btCallback?.onBluetoothState()
    }


    private fun onBluetoothDisconnected() {
        btCallback?.onBluetoothDisconnected()
    }

    private fun onBluetoothConnected() {
        btCallback?.onBluetoothConnected()
    }


    interface Callback {
        fun onBluetoothConnected()
        fun onBluetoothDisconnected()
        fun onBluetoothState()
    }


    companion object {
        fun register(callback: Callback, context: Context) {
            context.registerReceiver(BTBroadcast(callback), getFilter())
        }

        private fun getFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            return filter
        }
    }

}