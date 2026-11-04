package com.example.bluetoothremote.bluetooth

sealed class BluetoothState {
    object Disconnected : BluetoothState()
    object Connecting : BluetoothState()
    data class Connected(val deviceName: String) : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}
