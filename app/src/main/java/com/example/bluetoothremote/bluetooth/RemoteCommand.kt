package com.example.bluetoothremote.bluetooth

enum class RemoteCommand(
    val bytes: ByteArray,
    val hidKey: Byte,
    val displayChar: String
) {
    // Navigation
    UP(byteArrayOf(0x41), HidData.HID_KEY_UP, "UP"),
    DOWN(byteArrayOf(0x42), HidData.HID_KEY_DOWN, "DOWN"),
    LEFT(byteArrayOf(0x43), HidData.HID_KEY_LEFT, "LEFT"),
    RIGHT(byteArrayOf(0x44), HidData.HID_KEY_RIGHT, "RIGHT"),
    OK(byteArrayOf(0x45), HidData.HID_KEY_OK, "OK"),
    BACK(byteArrayOf(0x46), HidData.HID_KEY_BACK, "BACK"),
    HOME(byteArrayOf(0x47), HidData.HID_KEY_HOME, "HOME"),
    MENU(byteArrayOf(0x48), HidData.HID_KEY_MENU, "MENU"),

    // Media
    PLAY(byteArrayOf(0x49), HidData.HID_KEY_PLAY, "PLAY"),
    PAUSE(byteArrayOf(0x4A), HidData.HID_KEY_PLAY, "PAUSE"),
    STOP(byteArrayOf(0x4B), HidData.HID_KEY_BACK, "STOP"),

    // Volume
    VOLUME_UP(byteArrayOf(0x4C), HidData.HID_KEY_VOL_UP, "VOL+"),
    VOLUME_DOWN(byteArrayOf(0x4D), HidData.HID_KEY_VOL_DOWN, "VOL-"),
    MUTE(byteArrayOf(0x4E), HidData.HID_KEY_MUTE, "MUTE"),

    // Power
    POWER(byteArrayOf(0x50), HidData.HID_KEY_POWER, "POWER")
}
