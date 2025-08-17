package com.lanmusicsync.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lanmusicsync.R
import com.lanmusicsync.databinding.ActivityMainBinding
import com.lanmusicsync.model.Device
import com.lanmusicsync.model.PlaybackState
import com.lanmusicsync.service.MusicSyncService
import com.lanmusicsync.utils.NetworkUtils

class MainActivity : AppCompatActivity(), MusicSyncService.ServiceListener {
    
    private val tag = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    
    private var musicSyncService: MusicSyncService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicSyncService.MusicSyncBinder
            musicSyncService = binder.getService()
            musicSyncService?.setServiceListener(this@MainActivity)
            isServiceBound = true
            Log.d(tag, "Service connected")
            updateConnectionStatus()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            musicSyncService = null
            isServiceBound = false
            Log.d(tag, "Service disconnected")
            updateConnectionStatus()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
        checkPermissions()
        
        // Start and bind to service
        val serviceIntent = Intent(this, MusicSyncService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupClickListeners() {
        binding.cardHostMode.setOnClickListener {
            if (hasRequiredPermissions()) {
                showDeviceNameDialog(true)
            } else {
                requestPermissions()
            }
        }
        
        binding.cardClientMode.setOnClickListener {
            if (hasRequiredPermissions()) {
                showDeviceNameDialog(false)
            } else {
                requestPermissions()
            }
        }
        
        binding.btnSettings.setOnClickListener {
            // TODO: Open settings activity
            Toast.makeText(this, "设置功能即将推出", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabConnectionStatus.setOnClickListener {
            updateConnectionStatus()
            if (!NetworkUtils.isWifiConnected(this)) {
                Toast.makeText(this, "请连接到WiFi网络", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showDeviceNameDialog(isHost: Boolean) {
        val input = android.widget.EditText(this)
        input.hint = "输入设备名称"
        input.setText(android.os.Build.MODEL)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(if (isHost) "创建音乐房间" else "加入音乐房间")
            .setMessage("请输入您的设备名称")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val deviceName = input.text.toString().trim()
                if (deviceName.isNotEmpty()) {
                    if (isHost) {
                        startHostMode(deviceName)
                    } else {
                        startClientMode(deviceName)
                    }
                } else {
                    Toast.makeText(this, "请输入设备名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun startHostMode(deviceName: String) {
        musicSyncService?.let { service ->
            if (service.startAsHost(deviceName)) {
                val intent = Intent(this, HostActivity::class.java)
                intent.putExtra("device_name", deviceName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动主设备模式", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startClientMode(deviceName: String) {
        musicSyncService?.let { service ->
            if (service.startAsClient(deviceName)) {
                val intent = Intent(this, ClientActivity::class.java)
                intent.putExtra("device_name", deviceName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动从设备模式", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            showPermissionRationale()
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        return permissions
    }
    
    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要权限")
            .setMessage("此应用需要WiFi和位置权限来发现和连接其他设备，以及存储权限来访问音乐文件。")
            .setPositiveButton("授予权限") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun requestPermissions() {
        Dexter.withContext(this)
            .withPermissions(getRequiredPermissions())
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (it.areAllPermissionsGranted()) {
                            Toast.makeText(this@MainActivity, "权限已授予", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "需要所有权限才能正常使用", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }
    
    private fun updateConnectionStatus() {
        val isWifiConnected = NetworkUtils.isWifiConnected(this)
        val isServiceConnected = isServiceBound && musicSyncService != null
        
        when {
            !isWifiConnected -> {
                binding.fabConnectionStatus.setImageResource(R.drawable.ic_wifi_off)
                binding.fabConnectionStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.error_red)
            }
            isServiceConnected -> {
                binding.fabConnectionStatus.setImageResource(R.drawable.ic_music_note)
                binding.fabConnectionStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.success_green)
            }
            else -> {
                binding.fabConnectionStatus.setImageResource(R.drawable.ic_settings)
                binding.fabConnectionStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.warning_yellow)
            }
        }
    }
    
    // MusicSyncService.ServiceListener implementation
    override fun onPlaybackStateChanged(state: PlaybackState) {
        // Handle playback state changes if needed
    }
    
    override fun onDeviceConnected(device: Device) {
        runOnUiThread {
            Toast.makeText(this, "设备已连接: ${device.name}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDeviceDisconnected(deviceId: String) {
        runOnUiThread {
            Toast.makeText(this, "设备已断开连接", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDeviceDiscovered(device: Device) {
        // Handle device discovery if needed
    }
    
    override fun onSyncError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "同步错误: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

