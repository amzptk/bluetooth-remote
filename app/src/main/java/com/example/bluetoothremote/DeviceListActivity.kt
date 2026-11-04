package com.example.bluetoothremote

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothremote.databinding.ActivityDeviceListBinding
import com.example.bluetoothremote.databinding.ItemDeviceBinding

class DeviceListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var binding: ActivityDeviceListBinding
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupDeviceList()
    }

    private fun setupDeviceList() {
        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

            if (pairedDevices.isNullOrEmpty()) {
                Toast.makeText(this, "Tidak ada perangkat yang sudah dipasangkan. Pasangkan perangkat di Pengaturan Bluetooth.", Toast.LENGTH_LONG).show()
                return
            }

            val deviceList = pairedDevices.toList()
            val adapter = DeviceAdapter(deviceList) { device ->
                val intent = Intent()
                intent.putExtra(EXTRA_DEVICE_ADDRESS, device.address)
                setResult(RESULT_OK, intent)
                finish()
            }

            binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
            binding.recyclerDevices.adapter = adapter

        } catch (e: SecurityException) {
            Toast.makeText(this, "Izin Bluetooth tidak diberikan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

class DeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        try {
            holder.binding.tvDeviceName.text = device.name ?: "Unknown Device"
            holder.binding.tvDeviceAddress.text = device.address
            holder.itemView.setOnClickListener { onDeviceClick(device) }
        } catch (e: SecurityException) {
            holder.binding.tvDeviceName.text = "Unknown Device"
            holder.binding.tvDeviceAddress.text = device.address
        }
    }

    override fun getItemCount() = devices.size
}
