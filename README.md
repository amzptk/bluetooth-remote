# Bluetooth Remote Control App

Aplikasi remote control Bluetooth untuk mengendalikan Smart TV dan perangkat rumah pintar via Bluetooth Classic (RFCOMM).

## Fitur Utama

- 🔵 Scan dan temukan perangkat Bluetooth yang sudah dipasangkan
- 🔗 Koneksi ke perangkat via RFCOMM socket (Serial Port Profile)
- 🎮 UI remote control lengkap dengan tombol navigasi, media, dan volume
- 📶 Indikator status koneksi real-time
- 🔄 Tombol reconnect jika koneksi terputus
- 🌙 Dark mode Material Design 3

## Persyaratan

- **Android**: 6.0 (API 23) ke atas, direkomendasikan Android 13+ (Samsung S23)
- **Hardware**: Perangkat Android dengan Bluetooth
- **Izin**: BLUETOOTH_CONNECT, BLUETOOTH_SCAN (Android 12+), ACCESS_FINE_LOCATION (Android < 12)

## Cara Build

### Menggunakan Android Studio

1. Buka Android Studio (versi Hedgehog 2023.1.1 atau lebih baru)
2. Pilih **File → Open** dan arahkan ke folder `BluetoothRemote`
3. Tunggu Gradle sync selesai
4. Hubungkan perangkat Android atau gunakan emulator
5. Klik **Run** (▶) untuk build dan install

### Menggunakan Command Line

```bash
cd BluetoothRemote
./gradlew assembleDebug
# APK tersimpan di: app/build/outputs/apk/debug/app-debug.apk
```

## Cara Penggunaan

### 1. Pasangkan Perangkat Bluetooth Terlebih Dahulu

Sebelum menggunakan aplikasi:
1. Buka **Pengaturan → Bluetooth** di HP Android Anda
2. Aktifkan Bluetooth
3. Cari dan pasangkan (pair) dengan perangkat target (misalnya: modul HC-05/HC-06, Smart TV Bluetooth)
4. Masukkan PIN jika diminta (biasanya `1234` atau `0000` untuk modul HC-05)

### 2. Menggunakan Aplikasi

1. Buka aplikasi **Bluetooth Remote**
2. Tap tombol **"Hubungkan"**
3. Izinkan akses Bluetooth jika diminta
4. Pilih perangkat dari daftar perangkat yang sudah dipasangkan
5. Tunggu hingga status berubah menjadi **"Terhubung"** (indikator hijau)
6. Gunakan tombol remote untuk mengontrol perangkat

### 3. Jika Koneksi Terputus

- Tap **"Putuskan"** kemudian **"Hubungkan"** kembali
- Atau restart Bluetooth di pengaturan HP

## Tabel Perintah (Command Bytes)

Setiap tombol mengirimkan 1 byte data via Bluetooth:

| Tombol       | Hex  | ASCII | Keterangan         |
|-------------|------|-------|--------------------|
| UP ▲        | 0x41 | 'A'   | Navigasi atas      |
| DOWN ▼      | 0x42 | 'B'   | Navigasi bawah     |
| LEFT ◀      | 0x43 | 'C'   | Navigasi kiri      |
| RIGHT ▶     | 0x44 | 'D'   | Navigasi kanan     |
| OK/SELECT   | 0x45 | 'E'   | Konfirmasi/pilih   |
| BACK        | 0x46 | 'F'   | Kembali            |
| HOME 🏠      | 0x47 | 'G'   | Layar utama        |
| MENU ☰      | 0x48 | 'H'   | Menu               |
| PLAY ▶      | 0x49 | 'I'   | Putar media        |
| PAUSE ⏸     | 0x4A | 'J'   | Jeda media         |
| STOP ⏹      | 0x4B | 'K'   | Hentikan media     |
| VOLUME UP   | 0x4C | 'L'   | Volume naik        |
| VOLUME DOWN | 0x4D | 'M'   | Volume turun       |
| MUTE 🔇      | 0x4E | 'N'   | Bisukan            |
| POWER ⏻     | 0x50 | 'P'   | Daya ON/OFF        |

## Cara Kustomisasi Perintah

### Mengubah Byte Perintah

Edit file `RemoteCommand.kt`:

```kotlin
// Contoh: ubah perintah POWER menjadi string "POWER\n"
POWER("POWER\n".toByteArray())

// Atau gunakan byte sequence untuk protokol tertentu
UP(byteArrayOf(0xFF, 0x01, 0x00))
```

### Menambah Perintah Baru

1. Tambahkan entry baru di `RemoteCommand.kt`:
```kotlin
CHANNEL_UP(byteArrayOf(0x51)),   // 'Q'
CHANNEL_DOWN(byteArrayOf(0x52)) // 'R'
```

2. Tambahkan tombol baru di `activity_main.xml`

3. Hubungkan tombol di `MainActivity.kt`:
```kotlin
binding.btnChannelUp.setOnClickListener { sendCommand(RemoteCommand.CHANNEL_UP) }
```

### Kompatibilitas dengan Berbagai Perangkat

- **Modul HC-05/HC-06**: Gunakan byte ASCII (default sudah kompatibel)
- **Samsung Smart TV**: Mungkin membutuhkan format perintah khusus Samsung
- **LG ThinQ TV**: Gunakan format perintah LG Magic Remote
- **Arduino/ESP32**: Terima byte sesuai tabel di atas, proses dengan `Serial.read()`

## Arsitektur Aplikasi

```
BluetoothRemote/
├── app/
│   └── src/main/
│       ├── java/com/example/bluetoothremote/
│       │   ├── MainActivity.kt          # UI utama & logika remote
│       │   ├── DeviceListActivity.kt    # Daftar perangkat Bluetooth
│       │   └── bluetooth/
│       │       ├── BluetoothService.kt  # Koneksi & komunikasi BT
│       │       ├── BluetoothState.kt    # State koneksi (sealed class)
│       │       └── RemoteCommand.kt     # Definisi perintah (enum)
│       └── res/
│           ├── layout/                  # Layout XML
│           ├── values/                  # Colors, strings, themes
│           └── drawable/                # Indikator status
└── README.md
```

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| Perangkat tidak muncul di daftar | Pastikan sudah dipasangkan di Pengaturan Bluetooth |
| Gagal terhubung | Restart Bluetooth, pastikan perangkat target aktif |
| Perintah tidak diterima | Periksa byte perintah sesuai protokol perangkat target |
| Aplikasi crash | Pastikan izin Bluetooth sudah diberikan |

## Lisensi

Proyek ini dibuat untuk keperluan pembelajaran dan pengembangan. Bebas digunakan dan dimodifikasi.
