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
        // Standard SerialPortProfile UUID
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
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

                // Close existing connection
                closeSocket()

                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothAdapter?.cancelDiscovery()

                try {
                    socket?.connect()
                    outputStream = socket?.outputStream

                    val deviceName = try { device.name } catch (e: SecurityException) { deviceAddress }
                    _connectionState.value = BluetoothState.Connected(deviceName ?: deviceAddress)
                } catch (e: IOException) {
                    // Try fallback connection method
                    try {
                        val fallbackSocket = device.javaClass
                            .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            .invoke(device, 1) as BluetoothSocket
                        socket = fallbackSocket
                        fallbackSocket.connect()
                        outputStream = fallbackSocket.outputStream
                        val deviceName = try { device.name } catch (ex: SecurityException) { deviceAddress }
                        _connectionState.value = BluetoothState.Connected(deviceName ?: deviceAddress)
                    } catch (e2: Exception) {
                        closeSocket()
                        _connectionState.value = BluetoothState.Error("Gagal terhubung: ${e2.message}")
                    }
                }
            } catch (e: SecurityException) {
                _connectionState.value = BluetoothState.Error("Izin Bluetooth ditolak")
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
