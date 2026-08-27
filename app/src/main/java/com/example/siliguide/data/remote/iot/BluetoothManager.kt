package com.example.siliguide.data.remote.iot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class BluetoothManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    @param:ApplicationContext private val context: Context
) {
    private val systemBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager

    companion object {
        // Standard Serial Port Profile UUID
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    /**
     * Checks if the phone's Bluetooth radio is turned ON.
     */
    fun isBluetoothOn(): Boolean {
        return systemBluetoothManager?.adapter?.isEnabled == true
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    @SuppressLint("MissingPermission") // Temporary until runtime permissions are implemented
    @Throws(Exception::class)
    suspend fun connect(device: BluetoothDevice, timeoutMs: Long = 3000L) = withContext(Dispatchers.IO) {

        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this@BluetoothManager)} connecting to ${device.address} (${device.name})"
        )

        disconnect()

        bluetoothAdapter.cancelDiscovery()

        val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        socket = newSocket

        try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation {
                        try {
                            newSocket.close()
                        } catch (_: Exception) {}
                    }
                    try {
                        newSocket.connect()
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            }

            Log.d(
                "BluetoothManager",
                "Connected=${newSocket.isConnected}"
            )

            input = newSocket.inputStream
            output = newSocket.outputStream
            Log.d(
                "BluetoothManager",
                "Input=${input != null} Output=${output != null}"
            )

        } catch (e: Exception) {
            disconnect()
            throw e
        }
    }

    fun disconnect() {

        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this)}"
        )

        try {
            input?.close()
        } catch (_: Exception) {}

        try {
            output?.close()
        } catch (_: Exception) {}

        try {
            socket?.close()
        } catch (_: Exception) {}

        input = null
        output = null
        socket = null
    }

    @Throws(Exception::class)
    fun sendCommand(command: String) {

        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this)}"
        )

        Log.d(
            "BluetoothManager",
            "Before send: connected=${socket?.isConnected}, output=${output != null}"
        )
        val out = output ?: throw IllegalStateException("Not connected")

        out.write("$command\n".toByteArray())
        out.flush()

        Log.d(
            "BluetoothManager",
            "socket=$socket"
        )

        Log.d(
            "BluetoothManager",
            "input=$input"
        )

        Log.d(
            "BluetoothManager",
            "output=$output"
        )
    }

    @Throws(Exception::class)
    fun executeCommand(command: String): String {
        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this)} command=$command"
        )

        sendCommand(command)
        return readResponse()
    }

    @Throws(Exception::class)
    fun readResponse(): String {
        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this)}"
        )

        val inputStream = input ?: throw IllegalStateException("Not connected")

        val builder = StringBuilder()

        while (true) {
            val c = inputStream.read()

            if (c == -1)
                throw IllegalStateException("Bluetooth disconnected")

            if (c.toChar() == '\n')
                break

            builder.append(c.toChar())
        }

        return builder.toString()
    }
}