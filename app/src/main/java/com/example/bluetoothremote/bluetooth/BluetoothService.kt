package com.example.bluetoothremote.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothService(private val context: Context) {

    companion object {
        // Standard SerialPortProfile UUID (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<BluetoothState>(BluetoothState.Disconnected)
    val connectionState: StateFlow<BluetoothState> = _connectionState

    fun connect(deviceAddress: String) {
        serviceScope.launch {
            try {
                _connectionState.value = BluetoothState.Connecting

                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                    ?: run {
                        _connectionState.value = BluetoothState.Error("Perangkat tidak ditemukan")
                        return@launch
                    }

                // Batalkan discovery untuk menghemat bandwidth Bluetooth
                try {
                    bluetoothAdapter?.cancelDiscovery()
                } catch (e: SecurityException) {
                    // ignore
                }

                // Tutup koneksi lama dan beri jeda stack Bluetooth
                closeSocket()
                delay(150)

                var connected = false
                var lastErrorMessage = ""

                // Strategi 1 s.d 4: Coba berbagai mode soket RFCOMM (Insecure & Secure SPP serta Reflection)
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

                for ((name, createSocket) in strategies) {
                    var candidateSocket: BluetoothSocket? = null
                    try {
                        candidateSocket = createSocket()
                        candidateSocket.connect()

                        socket = candidateSocket
                        outputStream = candidateSocket.outputStream
                        val deviceName = try { device.name } catch (e: SecurityException) { deviceAddress }
                        _connectionState.value = BluetoothState.Connected(deviceName ?: deviceAddress)
                        connected = true
                        break
                    } catch (e: Exception) {
                        lastErrorMessage = e.message ?: "Koneksi gagal"
                        try {
                            candidateSocket?.close()
                        } catch (ignored: Exception) { }
                        delay(200)
                    }
                }

                // Strategi 5: Jika belum terhubung, coba UUID yang diiklankan oleh perangkat (jika ada)
                if (!connected) {
                    val parcelUuids = try { device.uuids } catch (e: SecurityException) { null }
                    if (parcelUuids != null) {
                        for (pUuid in parcelUuids) {
                            val targetUuid = pUuid.uuid
                            if (targetUuid == SPP_UUID) continue
                            var candidateSocket: BluetoothSocket? = null
                            try {
                                candidateSocket = device.createInsecureRfcommSocketToServiceRecord(targetUuid)
                                candidateSocket.connect()

                                socket = candidateSocket
                                outputStream = candidateSocket.outputStream
                                val deviceName = try { device.name } catch (e: SecurityException) { deviceAddress }
                                _connectionState.value = BluetoothState.Connected(deviceName ?: deviceAddress)
                                connected = true
                                break
                            } catch (e: Exception) {
                                lastErrorMessage = e.message ?: "Koneksi gagal"
                                try { candidateSocket?.close() } catch (ignored: Exception) { }
                                delay(200)
                            }
                        }
                    }
                }

                if (!connected) {
                    closeSocket()
                    _connectionState.value = BluetoothState.Error("Gagal terhubung ($lastErrorMessage). Pastikan perangkat target siap menerima koneksi Bluetooth SPP/Serial.")
                }

            } catch (e: SecurityException) {
                _connectionState.value = BluetoothState.Error("Izin Bluetooth ditolak")
            } catch (e: Exception) {
                closeSocket()
                _connectionState.value = BluetoothState.Error("Koneksi gagal: ${e.message}")
            }
        }
    }

    fun sendCommand(command: RemoteCommand): Boolean {
        return sendBytes(command.bytes)
    }

    fun sendBytes(bytes: ByteArray): Boolean {
        return try {
            outputStream?.write(bytes)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            _connectionState.value = BluetoothState.Disconnected
            false
        }
    }

    fun sendString(text: String): Boolean {
        val payload = if (text.endsWith("\n")) text else "$text\n"
        return sendBytes(payload.toByteArray(Charsets.UTF_8))
    }

    fun disconnect() {
        serviceScope.launch {
            closeSocket()
            _connectionState.value = BluetoothState.Disconnected
        }
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            // ignore
        } finally {
            outputStream = null
            socket = null
        }
    }
}
