package com.onenode.dropin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.onenode.dropin.service.ReceiverService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etCode: TextInputEditText
    private lateinit var etIp: TextInputEditText
    private lateinit var btnPair: Button
    private lateinit var btnUnlink: Button

    private val prefs by lazy {
        getSharedPreferences("OneNodePrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        etCode = findViewById(R.id.etCode)
        etIp = findViewById(R.id.etIp)
        btnPair = findViewById(R.id.btnPair)
        btnUnlink = findViewById(R.id.btnUnlink)

        btnPair.setOnClickListener { attemptPairing() }
        btnUnlink.setOnClickListener { unlink() }

        checkPermissions()
        updateUiState()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 100)
        }
    }

    private fun updateUiState() {
        val pairingToken = prefs.getString("pairing_token", null)
        val deviceName = prefs.getString("device_name", "Desktop")

        if (pairingToken != null) {
            tvStatus.text = "Linked to $deviceName"
            btnPair.visibility = View.GONE
            btnUnlink.visibility = View.VISIBLE
            etCode.isEnabled = false
            etIp.isEnabled = false
            
            // Start the service if paired
            val intent = Intent(this, ReceiverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            tvStatus.text = "Not linked"
            btnPair.visibility = View.VISIBLE
            btnUnlink.visibility = View.GONE
            etCode.isEnabled = true
            etIp.isEnabled = true
            btnPair.isEnabled = true
        }
    }

    private fun attemptPairing() {
        val code = etCode.text?.toString()?.trim() ?: ""
        val ip = etIp.text?.toString()?.trim() ?: ""

        if (code.length != 6) {
            Toast.makeText(this, "Enter a 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter the desktop IP address", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Connecting to One Node..."
        btnPair.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ip, 45678), 10000)
                    
                    val writer = socket.getOutputStream().bufferedWriter()
                    val reader = socket.getInputStream().bufferedReader()

                    val request = mapOf(
                        "code" to code,
                        "device" to Build.MODEL
                    )
                    writer.write(Gson().toJson(request))
                    writer.newLine()
                    writer.flush()

                    val response = reader.readLine()
                    socket.close()

                    if (response == null) throw Exception("No response from desktop")
                    Gson().fromJson(response, Map::class.java)
                }
            }

            result.onSuccess { response ->
                if (response["status"] == "ok") {
                    val token = response["token"] as String
                    prefs.edit()
                        .putString("pairing_token", token)
                        .putString("device_ip", ip)
                        .putString("device_name", "Desktop")
                        .apply()
                    
                    updateUiState()
                    Toast.makeText(this@MainActivity, "Linked successfully", Toast.LENGTH_SHORT).show()
                } else {
                    val message = response["message"] as? String ?: "Pairing failed"
                    tvStatus.text = "Error: $message"
                    btnPair.isEnabled = true
                }
            }.onFailure { e ->
                tvStatus.text = "Connection failed: ${e.message}"
                btnPair.isEnabled = true
            }
        }
    }

    private fun unlink() {
        prefs.edit().clear().apply()
        stopService(Intent(this, ReceiverService::class.java))
        updateUiState()
        etCode.text?.clear()
        etIp.text?.clear()
        Toast.makeText(this, "Unlinked from One Node", Toast.LENGTH_SHORT).show()
    }
}
