package com.example.siliguide.data.remote.iot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter
) {

    companion object {
        // Standard Serial Port Profile UUID
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    @SuppressLint("MissingPermission") // Temporary until runtime permissions are implemented
    @Throws(Exception::class)
    fun connect(device: BluetoothDevice) {

        Log.d(
            "BluetoothManager",
            "instance=${System.identityHashCode(this)}"
        )

        disconnect()

        bluetoothAdapter.cancelDiscovery()

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            socket?.connect()
            Log.d(
                "BluetoothManager",
                "Connected=${socket?.isConnected}"
            )

            input = socket?.inputStream
            output = socket?.outputStream
            Log.d(
                "BluetoothManager",
                "Input=${input != null} Output=${output != null}"
            )
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
            "instance=${System.identityHashCode(this)}"
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