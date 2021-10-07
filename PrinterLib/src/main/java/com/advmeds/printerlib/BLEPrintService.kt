package com.advmeds.printerlib

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

public class BLEPrintService(
    private val context: Context,
    public var delegate: BLEPrintServiceDelegate? = null
) {
    companion object {
        private const val TAG = "BLEPrintService"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    public var state: State = State.NONE
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
        if (state == State.CONNECTING) {
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

        state = State.NONE

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
            if (state !== State.CONNECTED) return
            r = mConnectedThread!!
        }

        // Perform the write unsynchronized
        r.write(bytes)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        state = State.NONE

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        state = State.NONE

        Handler(context.mainLooper).post {
            delegate?.onStateChanged(state)
        }
    }

    public interface BLEPrintServiceDelegate {
        fun onStateChanged(state: State)
    }

    public enum class State {
        /** we're doing nothing */
        NONE,

        /** now initiating an outgoing connection */
        CONNECTING,

        /** now connected to a remote device */
        CONNECTED;
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
            this@BLEPrintService.state = BLEPrintService.State.CONNECTING
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

            synchronized(this@BLEPrintService) {
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
            this@BLEPrintService.state = BLEPrintService.State.CONNECTED
        }

        override fun run() {
            var numBytes: Int

            while (this@BLEPrintService.state == BLEPrintService.State.CONNECTED) {
                Log.d("ConnectedThread", "123")

                try {
                    numBytes = mmInStream.read(mmBuffer)
                    Log.d("BluetoothSocket", numBytes.toString())
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