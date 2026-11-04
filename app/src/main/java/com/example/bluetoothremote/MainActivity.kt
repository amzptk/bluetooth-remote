package com.example.bluetoothremote

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bluetoothremote.bluetooth.BluetoothService
import com.example.bluetoothremote.bluetooth.BluetoothState
import com.example.bluetoothremote.bluetooth.RemoteCommand
import com.example.bluetoothremote.databinding.ActivityMainBinding
import com.example.bluetoothremote.shortcut.AppShortcut
import com.example.bluetoothremote.shortcut.ShortcutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothService: BluetoothService
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var shortcuts: MutableList<AppShortcut> = mutableListOf()

    // Touchpad Gesture State
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isSwiped = false
    private val SWIPE_THRESHOLD = 45f

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
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
        shortcuts = ShortcutManager.loadShortcuts(this)

        setupUI()
        setupNavigationModeTabs()
        setupTouchpad()
        setupShortcuts()
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

    private fun setupNavigationModeTabs() {
        binding.btnTabDpad.setOnClickListener {
            switchNavigationMode(isTouchpad = false)
        }

        binding.btnTabTouchpad.setOnClickListener {
            switchNavigationMode(isTouchpad = true)
        }
    }

    private fun switchNavigationMode(isTouchpad: Boolean) {
        if (isTouchpad) {
            binding.layoutDpad.visibility = View.GONE
            binding.layoutTouchpad.visibility = View.VISIBLE
            binding.btnTabTouchpad.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnTabTouchpad.setTextColor(Color.WHITE)
            binding.btnTabDpad.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabDpad.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.layoutDpad.visibility = View.VISIBLE
            binding.layoutTouchpad.visibility = View.GONE
            binding.btnTabDpad.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnTabDpad.setTextColor(Color.WHITE)
            binding.btnTabTouchpad.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabTouchpad.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchpad() {
        binding.layoutTouchpad.setOnTouchListener { view, event ->
            if (!binding.btnOk.isEnabled) {
                // Not connected
                return@setOnTouchListener false
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    isSwiped = false

                    // Position and show touch indicator
                    binding.touchIndicator.x = event.x - (binding.touchIndicator.width / 2f)
                    binding.touchIndicator.y = event.y - (binding.touchIndicator.height / 2f)
                    binding.touchIndicator.animate().alpha(0.7f).setDuration(100).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    binding.touchIndicator.x = event.x - (binding.touchIndicator.width / 2f)
                    binding.touchIndicator.y = event.y - (binding.touchIndicator.height / 2f)

                    val dx = event.x - touchStartX
                    val dy = event.y - touchStartY

                    if (!isSwiped && (abs(dx) > SWIPE_THRESHOLD || abs(dy) > SWIPE_THRESHOLD)) {
                        isSwiped = true
                        if (abs(dx) > abs(dy)) {
                            if (dx > 0) {
                                sendCommand(RemoteCommand.RIGHT)
                                pulseIndicator(binding.tvTouchRight)
                            } else {
                                sendCommand(RemoteCommand.LEFT)
                                pulseIndicator(binding.tvTouchLeft)
                            }
                        } else {
                            if (dy > 0) {
                                sendCommand(RemoteCommand.DOWN)
                                pulseIndicator(binding.tvTouchDown)
                            } else {
                                sendCommand(RemoteCommand.UP)
                                pulseIndicator(binding.tvTouchUp)
                            }
                        }
                        triggerHaptic(20)
                        touchStartX = event.x
                        touchStartY = event.y
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.touchIndicator.animate().alpha(0f).setDuration(150).start()

                    if (!isSwiped) {
                        // Single tap detected -> OK / Click
                        sendCommand(RemoteCommand.OK)
                        triggerHaptic(35)
                        pulseIndicator(binding.tvTouchpadStatus)
                    }
                    view.performClick()
                    true
                }

                else -> false
            }
        }
    }

    private fun pulseIndicator(target: View) {
        target.animate().alpha(1.0f).scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
            target.animate().alpha(0.4f).scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
        }.start()
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    private fun setupShortcuts() {
        refreshShortcutButtons()

        val buttons = listOf(
            binding.btnShortcut1,
            binding.btnShortcut2,
            binding.btnShortcut3,
            binding.btnShortcut4
        )

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (index < shortcuts.size) {
                    val sc = shortcuts[index]
                    sendShortcut(sc)
                }
            }

            button.setOnLongClickListener {
                showEditShortcutDialog(index)
                true
            }
        }

        binding.btnEditShortcuts.setOnClickListener {
            showSelectSlotToEditDialog()
        }
    }

    private fun refreshShortcutButtons() {
        val buttons = listOf(
            binding.btnShortcut1,
            binding.btnShortcut2,
            binding.btnShortcut3,
            binding.btnShortcut4
        )

        buttons.forEachIndexed { index, button ->
            if (index < shortcuts.size) {
                val sc = shortcuts[index]
                button.text = sc.name
                try {
                    val parsedColor = Color.parseColor(sc.colorHex)
                    button.backgroundTintList = ColorStateList.valueOf(parsedColor)
                } catch (e: Exception) {
                    button.backgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
                }
            }
        }
    }

    private fun sendShortcut(shortcut: AppShortcut) {
        triggerHaptic(25)
        if (bluetoothService.sendString(shortcut.command)) {
            showToast("Membuka ${shortcut.name}...")
        } else {
            showToast("Gagal mengirim perintah shortcut")
        }
    }

    private fun showSelectSlotToEditDialog() {
        val options = shortcuts.mapIndexed { idx, sc -> "Slot ${idx + 1}: ${sc.name}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Shortcut yang Ingin Diubah")
            .setItems(options) { _, which ->
                showEditShortcutDialog(which)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditShortcutDialog(slotIndex: Int) {
        if (slotIndex >= shortcuts.size) return
        val currentSc = shortcuts[slotIndex]

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_shortcut, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val spinnerPresets = dialogView.findViewById<Spinner>(R.id.spinnerPresets)
        val etName = dialogView.findViewById<EditText>(R.id.etShortcutName)
        val etCommand = dialogView.findViewById<EditText>(R.id.etShortcutCommand)

        tvTitle.text = "Ubah Shortcut (Slot ${slotIndex + 1})"
        etName.setText(currentSc.name)
        etCommand.setText(currentSc.command)

        val presetList = mutableListOf("Kustom (Manual)")
        presetList.addAll(ShortcutManager.PRESET_APPS.map { "${it.name} (${it.command})" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presetList)
        spinnerPresets.adapter = adapter

        var chosenColor = currentSc.colorHex

        spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val preset = ShortcutManager.PRESET_APPS[position - 1]
                    etName.setText(preset.name)
                    etCommand.setText(preset.command)
                    chosenColor = preset.colorHex
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName.text.toString().trim()
                val newCommand = etCommand.text.toString().trim()

                if (newName.isNotEmpty() && newCommand.isNotEmpty()) {
                    currentSc.name = newName
                    currentSc.command = newCommand
                    currentSc.colorHex = chosenColor
                    ShortcutManager.saveShortcuts(this, shortcuts)
                    refreshShortcutButtons()
                    showToast("Shortcut Slot ${slotIndex + 1} berhasil diperbarui!")
                } else {
                    showToast("Nama dan perintah tidak boleh kosong")
                }
            }
            .setNeutralButton("Reset Default") { _, _ ->
                shortcuts = ShortcutManager.getDefaultShortcuts().toMutableList()
                ShortcutManager.saveShortcuts(this, shortcuts)
                refreshShortcutButtons()
                showToast("Shortcut dikembalikan ke pengaturan default")
            }
            .setNegativeButton("Batal", null)
            .show()
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
            binding.btnVolUp, binding.btnVolDown, binding.btnMute, binding.btnPower,
            binding.btnShortcut1, binding.btnShortcut2, binding.btnShortcut3, binding.btnShortcut4
        ).forEach { btn ->
            btn.isEnabled = enabled
            btn.alpha = alpha
        }
        binding.layoutTouchpad.alpha = alpha
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
