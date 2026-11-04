package com.example.bluetoothremote.bluetooth

enum class RemoteCommand(val bytes: ByteArray) {
    // Navigation
    UP(byteArrayOf(0x41)),           // 'A'
    DOWN(byteArrayOf(0x42)),         // 'B'
    LEFT(byteArrayOf(0x43)),         // 'C'
    RIGHT(byteArrayOf(0x44)),        // 'D'
    OK(byteArrayOf(0x45)),           // 'E'
    BACK(byteArrayOf(0x46)),         // 'F'
    HOME(byteArrayOf(0x47)),         // 'G'
    MENU(byteArrayOf(0x48)),         // 'H'

    // Media
    PLAY(byteArrayOf(0x49)),         // 'I'
    PAUSE(byteArrayOf(0x4A)),        // 'J'
    STOP(byteArrayOf(0x4B)),         // 'K'

    // Volume
    VOLUME_UP(byteArrayOf(0x4C)),    // 'L'
    VOLUME_DOWN(byteArrayOf(0x4D)), // 'M'
    MUTE(byteArrayOf(0x4E)),        // 'N'

    // Power
    POWER(byteArrayOf(0x50))        // 'P'
}
