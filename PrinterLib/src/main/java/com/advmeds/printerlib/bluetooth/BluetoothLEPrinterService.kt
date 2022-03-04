package com.advmeds.printerlib.bluetooth

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BluetoothLEPrinterService : Service() {
    companion object {
        private const val TAG = "BLEPrinterService"
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLEPrinterService {
            return this@BluetoothLEPrinterService
        }
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: $newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    bluetoothGatt?.discoverServices()
                }
                else -> {

                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.d(TAG, "${gatt?.services}")
                }
                else -> {

                }
            }
        }
    }

    public var delegate: PrinterServiceDelegate? = null

    public var state: PrinterServiceDelegate.State = PrinterServiceDelegate.State.NONE
        private set

    fun connect(device: BluetoothDevice): Boolean {
        return try {
            // connect to the GATT server on the device
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)

            state = PrinterServiceDelegate.State.CONNECTING
            delegate?.onStateChanged(state)

            true
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Device not found with provided address.")
            false
        }
    }

    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }

        state = PrinterServiceDelegate.State.NONE
        delegate?.onStateChanged(state)
    }
}