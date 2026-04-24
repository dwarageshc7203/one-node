package com.onenode.dropin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.File
import java.net.ServerSocket

class ReceiverService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var serverSocket: ServerSocket? = null
    private var pingSocket: ServerSocket? = null
    private val CHANNEL_ID = "one_node_channel"
    private val PORT = 45679
    private val PING_PORT = 45681

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification("One Node — ready to receive files"))
        startListening()
        startPingServer()
    }

    private fun startListening() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                while (true) {
                    val client = serverSocket!!.accept()
                    launch { handleIncomingFile(client) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPingServer() {
        scope.launch {
            try {
                pingSocket = ServerSocket(PING_PORT)
                while (true) {
                    val client = pingSocket!!.accept()
                    launch {
                        try {
                            val reader = client.getInputStream().bufferedReader()
                            val writer = client.getOutputStream().bufferedWriter()
                            val msg = reader.readLine()
                            if (msg == "ping") {
                                writer.write("pong")
                                writer.newLine()
                                writer.flush()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            client.close()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleIncomingFile(socket: java.net.Socket) {
        try {
            socket.soTimeout = 30000 // 30 second timeout
            val input = DataInputStream(socket.getInputStream())

            // Read filename length (4 bytes)
            val nameLen = input.readInt()

            // Read filename
            val nameBytes = ByteArray(nameLen)
            input.readFully(nameBytes)
            val fileName = String(nameBytes)

            // Read file size (8 bytes)
            val fileSize = input.readLong()

            // Save to Downloads/OneNode/
            val dir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "OneNode")
            dir.mkdirs()

            val outFile = File(dir, fileName)
            val output = outFile.outputStream()
            val buffer = ByteArray(8192)
            var received = 0L

            while (received < fileSize) {
                val toRead = minOf(buffer.size.toLong(), fileSize - received).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break
                output.write(buffer, 0, read)
                received += read
            }

            output.flush()
            output.close()
            socket.close()

            notifyFileReceived(fileName)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyFileReceived(fileName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("One Node")
            .setContentText("File received: $fileName")
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("One Node")
            .setContentText(text)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "One Node", NotificationManager.IMPORTANCE_DEFAULT)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        serverSocket?.close()
        pingSocket?.close()
    }
}
