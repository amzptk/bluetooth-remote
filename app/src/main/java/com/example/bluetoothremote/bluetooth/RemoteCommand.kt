package com.example.bluetoothremote.bluetooth

enum class RemoteCommand(val bytes: ByteArray, val displayChar: String) {
    // Navigation
    UP(byteArrayOf(0x41), "A"),           // 'A'
    DOWN(byteArrayOf(0x42), "B"),         // 'B'
    LEFT(byteArrayOf(0x43), "C"),         // 'C'
    RIGHT(byteArrayOf(0x44), "D"),        // 'D'
    OK(byteArrayOf(0x45), "E"),           // 'E'
    BACK(byteArrayOf(0x46), "F"),         // 'F'
    HOME(byteArrayOf(0x47), "G"),         // 'G'
    MENU(byteArrayOf(0x48), "H"),         // 'H'

    // Media
    PLAY(byteArrayOf(0x49), "I"),         // 'I'
    PAUSE(byteArrayOf(0x4A), "J"),        // 'J'
    STOP(byteArrayOf(0x4B), "K"),         // 'K'

    // Volume
    VOLUME_UP(byteArrayOf(0x4C), "L"),    // 'L'
    VOLUME_DOWN(byteArrayOf(0x4D), "M"),  // 'M'
    MUTE(byteArrayOf(0x4E), "N"),         // 'N'

    // Power
    POWER(byteArrayOf(0x50), "P")         // 'P'
}
