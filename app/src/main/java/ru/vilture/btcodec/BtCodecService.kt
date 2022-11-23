package ru.vilture.btcodec

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*

class BtCodecService : Service() {
    var a2dpService: BluetoothA2dp? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    var codec = 999

    override fun onBind(intent: Intent?): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBlocking {
            GlobalScope.launch(Dispatchers.Default) {
                codec = readPrefValue(applicationContext, "MyCodec")
                if (codec == 999)
                    stopSelf()

                recoveryCodec()
                launch(Dispatchers.Main) {
                    Log.d("BtService", codec.toString())
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    suspend fun recoveryCodec() {
        val bluetoothManager =
            this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        getProfile(applicationContext)
        delay(2000)

        if (bluetoothAdapter != null && a2dpService != null && a2dpService!!.activeDevice != null) {
            val newConfig = BluetoothCodecConfig(
                codec,
                1000 * 1000,
                BluetoothCodecConfig.SAMPLE_RATE_44100,
                BluetoothCodecConfig.BITS_PER_SAMPLE_16,
                BluetoothCodecConfig.CHANNEL_MODE_STEREO,
                32771,
                49152,
                0,
                0
            )
            val deviceID: BluetoothDevice = a2dpService!!.activeDevice

            a2dpService!!.setCodecConfigPreference(deviceID, newConfig)
        }
    }

    fun readPrefValue(context: Context, key: String): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        return sharedPreferences.getInt(key, 999)
    }

    private fun getProfile(context: Context) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter!!.getProfileProxy(
                context,
                A2dpListener,
                BluetoothProfile.A2DP
            )
        }
    }

    private val A2dpListener: BluetoothProfile.ServiceListener = object :
        BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, a2dp: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpService = a2dp as BluetoothA2dp
            }
        }

        override fun onServiceDisconnected(profile: Int) {}
    }
}
