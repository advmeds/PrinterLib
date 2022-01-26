package com.advmeds.printerlib

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.util.*

public class BluetoothPrinterService(
    private val context: Context,
    public var delegate: PrinterServiceDelegate? = null
) {
    companion object {
        private const val TAG = "BluetoothPrinterService"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    public var state: PrinterServiceDelegate.State = PrinterServiceDelegate.State.NONE
        private set

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    @Synchronized
    public fun connect(device: BluetoothDevice) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (state == PrinterServiceDelegate.State.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread?.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        mConnectThread = ConnectThread(device)
        mConnectThread?.start()

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    @Synchronized
    public fun connected(socket: BluetoothSocket) {
        Log.d(TAG, "connected to: $socket")

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * Stop all threads
     */
    @Synchronized
    public fun disconnect() {
        Log.d(TAG, "disconnect")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        state = PrinterServiceDelegate.State.NONE

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param bytes The bytes to write
     * @see ConnectedThread.write
     */
    public fun write(bytes: ByteArray) {
        // Create temporary object
        var r: ConnectedThread
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state !== PrinterServiceDelegate.State.CONNECTED) return
            r = mConnectedThread!!
        }

        // Perform the write unsynchronized
        r.write(bytes)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        state = PrinterServiceDelegate.State.NONE

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        state = PrinterServiceDelegate.State.NONE

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(
        private val device: BluetoothDevice,
    ) : Thread() {
        private val mmSocket: BluetoothSocket by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
        }

        init {
            this@BluetoothPrinterService.state = PrinterServiceDelegate.State.CONNECTING
        }

        override fun run() {
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect()
            } catch (e: IOException) {
                Log.e(TAG, "Fail to connect", e)

                // Close the socket
                try {
                    mmSocket.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "Could not close the client socket", e2)
                }

                connectionFailed()
                return
            }

            synchronized(this@BluetoothPrinterService) {
                mConnectThread = null
            }

            connected(mmSocket)
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(
        private val socket: BluetoothSocket,
    ) : Thread() {
        private val mmInStream = socket.inputStream
        private val mmOutStream = socket.outputStream
        private val mmBuffer = ByteArray(1024)

        init {
            this@BluetoothPrinterService.state = PrinterServiceDelegate.State.CONNECTED
        }

        override fun run() {
            var numBytes: Int

            while (this@BluetoothPrinterService.state == PrinterServiceDelegate.State.CONNECTED) {
                try {
                    numBytes = mmInStream.read(mmBuffer)
                    Log.d(TAG, "read: ${mmBuffer.copyOf(numBytes).decodeToString()}")
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param bytes The bytes to write
         */
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }
}