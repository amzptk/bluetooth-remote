package com.example.bluetoothremote.bluetooth

object HidData {

    // Combined HID Report Descriptor: Keyboard (Report ID 1) + Mouse (Report ID 2)
    val HID_DESCRIPTOR = byteArrayOf(
        // -----------------------------------------------------------------
        // 1. KEYBOARD (Report ID = 1)
        // -----------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) - Modifier Keys
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Ary,Abs) - Reserved
        0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0xFF.toByte(), //   LOGICAL_MAXIMUM (255)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0xFF.toByte(), //   USAGE_MAXIMUM (Key 255)
        0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs) - Key Array
        0xC0.toByte(),                // END_COLLECTION

        // -----------------------------------------------------------------
        // 2. MOUSE (Report ID = 2)
        // -----------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs) - Buttons
        0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(), //     INPUT (Cnst,Ary,Abs) - Padding
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel) - X, Y coordinates
        0x09.toByte(), 0x38.toByte(), //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel) - Wheel
        0xC0.toByte(),                //   END_COLLECTION
        0xC0.toByte()                 // END_COLLECTION
    )

    // Standard USB HID Usage Page 0x07 Keycodes for Android STB / TV
    const val HID_KEY_UP: Byte = 0x52.toByte()         // UpArrow -> DPAD_UP
    const val HID_KEY_DOWN: Byte = 0x51.toByte()       // DownArrow -> DPAD_DOWN
    const val HID_KEY_LEFT: Byte = 0x50.toByte()       // LeftArrow -> DPAD_LEFT
    const val HID_KEY_RIGHT: Byte = 0x4F.toByte()      // RightArrow -> DPAD_RIGHT
    const val HID_KEY_OK: Byte = 0x28.toByte()         // Return/Enter -> DPAD_CENTER / ENTER
    const val HID_KEY_BACK: Byte = 0x29.toByte()       // Escape -> KEYCODE_BACK
    const val HID_KEY_HOME: Byte = 0x4A.toByte()       // Home -> KEYCODE_HOME
    const val HID_KEY_MENU: Byte = 0x65.toByte()       // App/Menu -> KEYCODE_MENU
    const val HID_KEY_VOL_UP: Byte = 0x80.toByte()     // Volume Up -> KEYCODE_VOLUME_UP
    const val HID_KEY_VOL_DOWN: Byte = 0x81.toByte()   // Volume Down -> KEYCODE_VOLUME_DOWN
    const val HID_KEY_MUTE: Byte = 0x7F.toByte()       // Mute -> KEYCODE_VOLUME_MUTE
    const val HID_KEY_POWER: Byte = 0x66.toByte()      // Power -> KEYCODE_POWER
    const val HID_KEY_PLAY: Byte = 0x2C.toByte()       // Space -> KEYCODE_MEDIA_PLAY_PAUSE
}
