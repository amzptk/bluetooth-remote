package com.example.bluetoothremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bluetoothremote.bluetooth.BluetoothService
import com.example.bluetoothremote.bluetooth.BluetoothState
import com.example.bluetoothremote.bluetooth.RemoteCommand
import com.example.bluetoothremote.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothService: BluetoothService
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            openDeviceList()
        } else {
            showToast("Bluetooth harus diaktifkan untuk menggunakan remote")
        }
    }

    private val deviceListLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
            if (address != null) {
                bluetoothService.connect(address)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkBluetoothAndConnect()
        } else {
            showToast("Izin Bluetooth diperlukan untuk menggunakan remote")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothService = BluetoothService(this)
        setupUI()
        observeBluetoothState()
    }

    private fun setupUI() {
        // Connection button
        binding.btnConnect.setOnClickListener {
            checkPermissionsAndConnect()
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothService.disconnect()
        }

        // Navigation buttons
        binding.btnUp.setOnClickListener { sendCommand(RemoteCommand.UP) }
        binding.btnDown.setOnClickListener { sendCommand(RemoteCommand.DOWN) }
        binding.btnLeft.setOnClickListener { sendCommand(RemoteCommand.LEFT) }
        binding.btnRight.setOnClickListener { sendCommand(RemoteCommand.RIGHT) }
        binding.btnOk.setOnClickListener { sendCommand(RemoteCommand.OK) }
        binding.btnBack.setOnClickListener { sendCommand(RemoteCommand.BACK) }
        binding.btnHome.setOnClickListener { sendCommand(RemoteCommand.HOME) }
        binding.btnMenu.setOnClickListener { sendCommand(RemoteCommand.MENU) }

        // Media buttons
        binding.btnPlay.setOnClickListener { sendCommand(RemoteCommand.PLAY) }
        binding.btnPause.setOnClickListener { sendCommand(RemoteCommand.PAUSE) }
        binding.btnStop.setOnClickListener { sendCommand(RemoteCommand.STOP) }

        // Volume buttons
        binding.btnVolUp.setOnClickListener { sendCommand(RemoteCommand.VOLUME_UP) }
        binding.btnVolDown.setOnClickListener { sendCommand(RemoteCommand.VOLUME_DOWN) }
        binding.btnMute.setOnClickListener { sendCommand(RemoteCommand.MUTE) }

        // Power button
        binding.btnPower.setOnClickListener { sendCommand(RemoteCommand.POWER) }
    }

    private fun observeBluetoothState() {
        lifecycleScope.launch {
            bluetoothService.connectionState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: BluetoothState) {
        when (state) {
            is BluetoothState.Disconnected -> {
                binding.tvStatus.text = "Tidak Terhubung"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
                binding.tvDeviceName.text = ""
                binding.tvDeviceName.visibility = View.GONE
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
                setRemoteButtonsEnabled(false)
                binding.statusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
            }
            is BluetoothState.Connecting -> {
                binding.tvStatus.text = "Menghubungkan..."
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connecting))
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = false
                setRemoteButtonsEnabled(false)
                binding.statusIndicator.setBackgroundResource(R.drawable.indicator_connecting)
            }
            is BluetoothState.Connected -> {
                binding.tvStatus.text = "Terhubung"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
                binding.tvDeviceName.text = state.deviceName
                binding.tvDeviceName.visibility = View.VISIBLE
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
                setRemoteButtonsEnabled(true)
                binding.statusIndicator.setBackgroundResource(R.drawable.indicator_connected)
            }
            is BluetoothState.Error -> {
                binding.tvStatus.text = "Error: ${state.message}"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
                setRemoteButtonsEnabled(false)
                binding.statusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
                showToast(state.message)
            }
        }
    }

    private fun setRemoteButtonsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.4f
        listOf(
            binding.btnUp, binding.btnDown, binding.btnLeft, binding.btnRight,
            binding.btnOk, binding.btnBack, binding.btnHome, binding.btnMenu,
            binding.btnPlay, binding.btnPause, binding.btnStop,
            binding.btnVolUp, binding.btnVolDown, binding.btnMute, binding.btnPower
        ).forEach { btn ->
            btn.isEnabled = enabled
            btn.alpha = alpha
        }
    }

    private fun sendCommand(command: RemoteCommand) {
        if (!bluetoothService.sendCommand(command)) {
            showToast("Gagal mengirim perintah")
        }
    }

    private fun checkPermissionsAndConnect() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            checkBluetoothAndConnect()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun checkBluetoothAndConnect() {
        if (bluetoothAdapter == null) {
            AlertDialog.Builder(this)
                .setTitle("Bluetooth Tidak Tersedia")
                .setMessage("Perangkat ini tidak mendukung Bluetooth")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableIntent)
        } else {
            openDeviceList()
        }
    }

    private fun openDeviceList() {
        val intent = Intent(this, DeviceListActivity::class.java)
        deviceListLauncher.launch(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}
