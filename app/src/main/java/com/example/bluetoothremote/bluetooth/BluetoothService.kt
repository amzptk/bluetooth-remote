package com.example.bluetoothremote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothService(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val REPORT_ID_KEYBOARD = 1
        private const val REPORT_ID_MOUSE = 2
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    // Bluetooth HID Device Profile (Untuk STB, Smart TV, Android Box, PC)
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHidDevice: BluetoothDevice? = null
    private var isHidRegistered = false

    // Bluetooth Classic RFCOMM / SPP Socket (Fallback untuk Arduino / HC-05)
    private var sppSocket: BluetoothSocket? = null
    private var sppOutputStream: OutputStream? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<BluetoothState>(BluetoothState.Disconnected)
    val connectionState: StateFlow<BluetoothState> = _connectionState

    private val hidCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                isHidRegistered = registered
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedHidDevice = device
                        val name = try { device?.name } catch (e: SecurityException) { null } ?: "STB / Smart TV"
                        _connectionState.value = BluetoothState.Connected(name)
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _connectionState.value = BluetoothState.Connecting
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (connectedHidDevice == device) {
                            connectedHidDevice = null
                            if (sppSocket == null) {
                                _connectionState.value = BluetoothState.Disconnected
                            }
                        }
                    }
                }
            }
        }
    } else null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                hidDevice = proxy as? BluetoothHidDevice
                registerHidProfile()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                isHidRegistered = false
                connectedHidDevice = null
            }
        }
    }

    init {
        // Inisialisasi HID Profile Proxy
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
            }
        } catch (e: Exception) {
            // Ignore if HID profile proxy is not supported on this hardware
        }
    }

    private fun registerHidProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val deviceService = hidDevice ?: return
        if (isHidRegistered) return

        try {
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Bluetooth Remote",
                "STB & Smart TV Controller",
                "Android",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidData.HID_DESCRIPTOR
            )

            deviceService.registerApp(
                sdpSettings,
                null,
                null,
                Executors.newCachedThreadPool(),
                hidCallback ?: return
            )
        } catch (e: Exception) {
            // Ignore if registration fails
        }
    }

    fun connect(deviceAddress: String) {
        serviceScope.launch {
            try {
                _connectionState.value = BluetoothState.Connecting

                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                    ?: run {
                        _connectionState.value = BluetoothState.Error("Perangkat tidak ditemukan")
                        return@launch
                    }

                try {
                    bluetoothAdapter?.cancelDiscovery()
                } catch (e: SecurityException) { }

                // 1. Hubungkan via Bluetooth HID Device (Prioritas Utama untuk STB / Smart TV)
                var hidConnected = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hidDevice != null) {
                    if (!isHidRegistered) {
                        registerHidProfile()
                        delay(250)
                    }

                    try {
                        connectedHidDevice = device
                        val result = hidDevice?.connect(device) ?: false
                        if (result) {
                            val deviceName = try { device.name } catch (e: SecurityException) { null } ?: deviceAddress
                            _connectionState.value = BluetoothState.Connected(deviceName)
                            hidConnected = true
                        }
                    } catch (e: Exception) {
                        connectedHidDevice = null
                    }
                }

                // 2. Hubungkan juga via SPP Serial (untuk kompatibilitas mikrokontroler / receiver)
                connectSpp(device, deviceAddress, hidConnected)

            } catch (e: SecurityException) {
                _connectionState.value = BluetoothState.Error("Izin Bluetooth ditolak")
            } catch (e: Exception) {
                _connectionState.value = BluetoothState.Error("Koneksi gagal: ${e.message}")
            }
        }
    }

    private suspend fun connectSpp(device: BluetoothDevice, deviceAddress: String, isHidAlreadyConnected: Boolean) {
        closeSppSocket()
        delay(150)

        val strategies: List<Pair<String, () -> BluetoothSocket>> = listOf(
            "Insecure SPP" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            "Standard SPP" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            "Reflection Insecure Ch1" to {
                device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            },
            "Reflection Secure Ch1" to {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            }
        )

        var sppConnected = false
        for ((_, createSocket) in strategies) {
            var candidateSocket: BluetoothSocket? = null
            try {
                candidateSocket = createSocket()
                candidateSocket.connect()
                val stream = candidateSocket.outputStream
                if (stream != null) {
                    sppSocket = candidateSocket
                    sppOutputStream = stream
                    val deviceName = try { device.name } catch (e: SecurityException) { null } ?: deviceAddress
                    _connectionState.value = BluetoothState.Connected(deviceName)
                    sppConnected = true
                    break
                } else {
                    candidateSocket.close()
                }
            } catch (e: Exception) {
                try { candidateSocket?.close() } catch (ignored: Exception) { }
                delay(150)
            }
        }

        if (!sppConnected && !isHidAlreadyConnected) {
            _connectionState.value = BluetoothState.Error("Gagal terhubung ke STB. Pastikan STB sudah dipasangkan di Pengaturan Bluetooth HP.")
        }
    }

    /**
     * Mengirim perintah remote:
     * - Mengirim HID Keystroke ke STB (DPAD_UP, DPAD_DOWN, ENTER, BACK, HOME, VOL+, POWER)
     * - Sekaligus mengirim byte Serial SPP jika soket serial aktif
     */
    fun sendCommand(command: RemoteCommand): Boolean {
        var sent = false

        // 1. Kirim sinyal HID (Asli untuk STB / Smart TV)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val device = connectedHidDevice
            val hid = hidDevice
            if (device != null && hid != null) {
                try {
                    // Report ID 1: [Modifier, Reserved, Key1, Key2, Key3, Key4, Key5, Key6]
                    val pressReport = byteArrayOf(0, 0, command.hidKey, 0, 0, 0, 0, 0)
                    hid.sendReport(device, REPORT_ID_KEYBOARD, pressReport)

                    // Lepas tombol otomatis setelah 50ms
                    serviceScope.launch {
                        delay(50)
                        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                        hid.sendReport(device, REPORT_ID_KEYBOARD, releaseReport)
                    }
                    sent = true
                } catch (e: Exception) { }
            }
        }

        // 2. Kirim serial byte (untuk Arduino / modul SPP)
        val stream = sppOutputStream
        if (stream != null) {
            try {
                stream.write(command.bytes)
                stream.flush()
                sent = true
            } catch (e: Exception) {
                closeSppSocket()
            }
        }

        return sent
    }

    fun sendString(text: String): Boolean {
        val payload = if (text.endsWith("\n")) text else "$text\n"
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val stream = sppOutputStream ?: return false
        return try {
            stream.write(bytes)
            stream.flush()
            true
        } catch (e: Exception) {
            closeSppSocket()
            false
        }
    }

    fun disconnect() {
        serviceScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                connectedHidDevice?.let { dev ->
                    try { hidDevice?.disconnect(dev) } catch (e: Exception) { }
                }
                connectedHidDevice = null
            }
            closeSppSocket()
            _connectionState.value = BluetoothState.Disconnected
        }
    }

    private fun closeSppSocket() {
        try {
            sppOutputStream?.close()
            sppSocket?.close()
        } catch (e: IOException) { }
        finally {
            sppOutputStream = null
            sppSocket = null
        }
    }
}
