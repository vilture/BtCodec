package ru.vilture.btcodec

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.*
import android.bluetooth.BluetoothCodecConfig.*
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import ru.vilture.btcodec.databinding.ActivityMainBinding
import java.util.*


@OptIn(DelicateCoroutinesApi::class)
class MainActivity : AppCompatActivity(), BTBroadcast.Callback {


    private lateinit var binding: ActivityMainBinding

    var a2dpService: BluetoothA2dp? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    var devices: MutableList<BluetoothDevice>? = null
    private var deviceID = ""
    private var device = ""
    private var codec = ""
    private var isA2dpReady = false

    var BLUETOOTH_REQ_CODE = 1

    private val A2dpListener: ServiceListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, a2dp: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpService = a2dp as BluetoothA2dp

                devices = initA2DP()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            setIsA2dpReady(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent()
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }


        binding.power.text = "BT OFF"
        binding.power.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

        binding.sbc.isEnabled = false
        binding.aac.isEnabled = false
        binding.lhdc.isEnabled = false
        binding.device.text = "Нет устройств"
        device = ""

        val serviceClass = BtCodecService::class.java
        val service = Intent(this, BtCodecService::class.java)
        if (!isServiceRunning(serviceClass)) {
            startService(service)
        }

        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            BTBroadcast.register(this, this)
        }

        bindingBTA(this)

        binding.power.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_DENIED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 2
                    )
                    return@setOnClickListener
                }
            }

            if (!bluetoothAdapter?.isEnabled!!) {

                val blueToothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(blueToothIntent, BLUETOOTH_REQ_CODE)

                bindingBTA(this)
            } else {
                bluetoothAdapter!!.disable()
                binding.power.text = "BT OFF"
                binding.power.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                binding.sbc.isEnabled = false
                binding.aac.isEnabled = false
                binding.lhdc.isEnabled = false
                binding.device.text = "Нет устройств"
                device = ""
            }
        }


        binding.power.setOnLongClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            true
        }

        binding.sbc.setOnClickListener {
            val deviceID: BluetoothDevice = a2dpService!!.activeDevice
            val status: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            val config = status.codecConfig
            val newConfig = BluetoothCodecConfig(
                0,
                1000 * 1000,
                SAMPLE_RATE_44100,
                BITS_PER_SAMPLE_16,
                CHANNEL_MODE_STEREO,
                32771,
                49152,
                0,
                0
            )

            savePrefValue("MyCodec", newConfig.codecType)
            a2dpService!!.setCodecConfigPreference(deviceID, newConfig)
            val updatedStatus: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            codec = getCCodec(newConfig.toString())

            binding.device.text = "$device ( $codec )"
            Toast.makeText(this, "Подключились как $codec", Toast.LENGTH_SHORT).show()
        }

        binding.aac.setOnClickListener {
            val deviceID: BluetoothDevice = a2dpService!!.activeDevice
            val status: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            val config = status.codecConfig
            val newConfig = BluetoothCodecConfig(
                1,
                1000 * 1000,
                SAMPLE_RATE_44100,
                BITS_PER_SAMPLE_16,
                CHANNEL_MODE_STEREO,
                32771,
                49152,
                0,
                0
            )

            savePrefValue("MyCodec", newConfig.codecType)
            a2dpService!!.setCodecConfigPreference(deviceID, newConfig)
            val updatedStatus: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            codec = getCCodec(newConfig.toString())

            binding.device.text = "$device ( $codec )"
            Toast.makeText(this, "Подключились как $codec", Toast.LENGTH_SHORT).show()
        }

        binding.lhdc.setOnClickListener {
            val deviceID: BluetoothDevice = a2dpService!!.activeDevice
            val status: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            val config = status.codecConfig
            val newConfig = BluetoothCodecConfig(
                9,
                1000 * 1000,
                SAMPLE_RATE_44100,
                BITS_PER_SAMPLE_16,
                CHANNEL_MODE_STEREO,
                32771,
                49152,
                0,
                0
            )

            savePrefValue("MyCodec", newConfig.codecType)
            a2dpService!!.setCodecConfigPreference(deviceID, newConfig)
            val updatedStatus: BluetoothCodecStatus = a2dpService!!.getCodecStatus(deviceID)
            codec = getCCodec(newConfig.toString())

            binding.device.text = "$device ( $codec )"
            Toast.makeText(this, "Подключились как $codec", Toast.LENGTH_SHORT).show()

        }

    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            } else {
                initA2DP()
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {

            }
        }


    private fun initA2DP(): MutableList<BluetoothDevice>? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }


        val devices = if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return devices
        } else {
            a2dpService!!.connectedDevices
        }

        for (d in devices) {
            deviceID = d.address
            device = d.name
        }

        return devices
    }


    fun savePrefValue(key: String, value: Int) {
        val sPref = PreferenceManager.getDefaultSharedPreferences(this)
        val ed = sPref!!.edit()

        ed.putInt(key, value)
        ed.apply()
    }


    @SuppressLint("MissingPermission")
    fun bindingBTA(context: Context) {
        runBlocking {
            GlobalScope.launch(Dispatchers.Default) {
                getProfile(context)
                delay(2000)

                if (bluetoothAdapter != null && a2dpService != null) {
                    launch(Dispatchers.Main) {

                        if (deviceID.isNotEmpty()) Log.d("BT", "A2DP готов для $deviceID")
                        else Toast.makeText(
                            context, "Устройство A2DP не найдено", Toast.LENGTH_SHORT
                        ).show()

                        if (!bluetoothAdapter!!.isEnabled) {
                            binding.power.text = "BT OFF"
                            binding.power.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                            binding.sbc.isEnabled = false
                            binding.aac.isEnabled = false
                            binding.lhdc.isEnabled = false
                            binding.device.text = "Нет устройств"
                            device = ""
                        } else {
                            if (a2dpService!!.activeDevice != null) {

                                getSaveCodec(context)

                                if (codec == "")
                                    codec = getCCodec(
                                        a2dpService!!.getCodecStatus(a2dpService!!.activeDevice).codecConfig.toString()
                                    )
                            }

                            binding.power.text = "BT ON"
                            binding.power.backgroundTintList = ColorStateList.valueOf(Color.BLUE)

                            binding.sbc.isEnabled = true
                            binding.aac.isEnabled = true
                            binding.lhdc.isEnabled = true

                            binding.device.text = "$device ( $codec )"
                        }

                        if (devices?.isEmpty() == true) {
                            binding.sbc.isEnabled = false
                            binding.aac.isEnabled = false
                            binding.lhdc.isEnabled = false
                            binding.device.text = "Нет устройств"
                            device = ""
                        }
                    }
                }


            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSaveCodec(context: Context) {
        var myCodec = 999
        myCodec = BtCodecService().readPrefValue(context, "MyCodec")
        if (myCodec != 999) {
            val newConfig = BluetoothCodecConfig(
                myCodec,
                1000 * 1000,
                SAMPLE_RATE_44100,
                BITS_PER_SAMPLE_16,
                CHANNEL_MODE_STEREO,
                32771,
                49152,
                0,
                0
            )
            val deviceID: BluetoothDevice = a2dpService!!.activeDevice

            a2dpService!!.setCodecConfigPreference(deviceID, newConfig)
            a2dpService!!.getCodecStatus(deviceID)
            codec = getCCodec(newConfig.toString())
        }
    }

    private fun getCCodec(find: String): String {
        return if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return " "
        } else {
            Regex(""":[a-zA-Z]+""").find(find)?.value?.substring(1).toString()
        }
    }


    fun setIsA2dpReady(ready: Boolean) {
        isA2dpReady = ready
        Toast.makeText(this, "A2DP " + if (ready) "готов" else "выключен", Toast.LENGTH_SHORT)
            .show()
    }

    private fun getProfile(context: Context) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter!!.getProfileProxy(
                context, A2dpListener, BluetoothProfile.A2DP
            )
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val context = this

        if (resultCode == RESULT_OK) {
            GlobalScope.launch(Dispatchers.Default) {
                delay(2000)
                getProfile(context)

                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Bluetooth включен", Toast.LENGTH_SHORT)
                        .show()

                    if (a2dpService!!.activeDevice != null) {

                        getSaveCodec(this@MainActivity)
                        if (codec == "")
                            codec = getCCodec(
                                a2dpService!!.getCodecStatus(a2dpService!!.activeDevice).codecConfig.toString()
                            )

                        if (device == "") device = a2dpService!!.activeDevice.name

                        binding.power.text = "BT ON"
                        binding.power.backgroundTintList = ColorStateList.valueOf(Color.BLUE)

                        binding.sbc.isEnabled = true
                        binding.aac.isEnabled = true
                        binding.lhdc.isEnabled = true
                        binding.device.text = "$device ( $codec )"
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(
                this@MainActivity, "Работа Bluetooth отменена", Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onBluetoothConnected() {
        val context = this
        GlobalScope.launch(Dispatchers.Default) {
            delay(2000)
            getProfile(context)

            launch(Dispatchers.Main) {
                if (a2dpService!!.activeDevice != null) {

                    getSaveCodec(context)

                    if (codec == "")
                        codec = getCCodec(
                            a2dpService!!.getCodecStatus(a2dpService!!.activeDevice).codecConfig.toString()
                        )
                    if (device == "") device = a2dpService!!.activeDevice.name
                }

                binding.sbc.isEnabled = true
                binding.aac.isEnabled = true
                binding.lhdc.isEnabled = true
                binding.device.text = "$device ( $codec )"

                Toast.makeText(context, "Подключено $device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBluetoothDisconnected() {
        binding.sbc.isEnabled = false
        binding.aac.isEnabled = false
        binding.lhdc.isEnabled = false
        binding.device.text = "Нет устройств"
        device = ""

        Toast.makeText(this, "Отключено", Toast.LENGTH_SHORT).show()
    }

}