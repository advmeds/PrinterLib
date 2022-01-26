package com.advmeds.printerlibdemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.advmeds.printerlib.PrinterBuffer
import com.advmeds.printerlib.PrinterServiceDelegate
import android.content.*
import android.hardware.usb.UsbConstants
import androidx.activity.result.contract.ActivityResultContracts
import com.advmeds.printerlib.BluetoothPrinterService
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val mainRecyclerView: RecyclerView by lazy { findViewById(R.id.main_rv) }
    private val mainAdapter = MainAdapter { position ->
        Log.d("onItemClick", position.toString())

        val device = devices[position]
        printService.connect(device)
    }
    private var devices = mutableListOf<BluetoothDevice>()

    /** 確認使用者授權的Callback */
    private val bleForResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.find { !it } == null) {
                enableBluetoothService()
            }
        }

    /** Create a BroadcastReceiver for ACTION_STATE_CHANGED. */
    private val detectBluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0)
                    val currState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    Log.d("ACTION_STATE_CHANGED", "prevState: $prevState, currState: $currState")

                    when (currState) {
                        BluetoothAdapter.STATE_ON -> {
                            startScan()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            stopScan()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            BluetoothAdapter.getDefaultAdapter().enable()
                        }
                    }
                }
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val detectBluetoothDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    device ?: return

                    if (!devices.map { it.address }.contains(device.address)) {
                        Log.d("ACTION_FOUND", device.name)
                        devices.add(device)
                        mainAdapter.dataSet = devices.map { it.name }
                        mainAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private val printCallback = object : PrinterServiceDelegate {
        override fun onStateChanged(state: PrinterServiceDelegate.State) {
            Log.d("onStateChanged", state.toString())

            when (state) {
                PrinterServiceDelegate.State.NONE -> {
                    startScan()
                }
                PrinterServiceDelegate.State.CONNECTING -> {
                    stopScan()
                }
                PrinterServiceDelegate.State.CONNECTED -> {
                    val printer = PrinterBuffer()
                    printer.appendText(
                        "煩請親自依「掛號燈號」至櫃台掛號。過號或號碼單遺失者，請重新抽號。",
                        PrinterBuffer.TextAlignment.LEFT,
                        PrinterBuffer.FontSize.MIDDLE
                    )
                    printer.appendDivider()
                    printer.appendText(
                        "掛完號請依「就診燈號」看診。謝謝您的合作！",
                        PrinterBuffer.TextAlignment.LEFT,
                        PrinterBuffer.FontSize.MIDDLE
                    )
                    printer.appendText(
                        String.format("%04d", 69),
                        PrinterBuffer.TextAlignment.CENTER,
                        PrinterBuffer.FontSize.BIG
                    )
                    val now = Date()
                    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN)
                    formatter.timeZone = TimeZone.getTimeZone("Asia/Taipei")
                    printer.appendText(
                        formatter.format(now),
                        PrinterBuffer.TextAlignment.CENTER
                    )
                    printer.appendNewLine()

                    printService.write(printer.data)
                }
            }
        }
    }

    private lateinit var printService: BluetoothPrinterService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecyclerView.adapter = mainAdapter

        val bluetoothStateFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(
            detectBluetoothStateReceiver,
            bluetoothStateFilter
        )

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(detectBluetoothDeviceReceiver, filter)

        requestBluetoothPermissions()

        printService = BluetoothPrinterService(this, printCallback)
    }

    override fun onResume() {
        super.onResume()

        requestBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()

        stopScan()
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        bleForResult.launch(permissions)
    }

    private fun enableBluetoothService() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter.isEnabled) {
            startScan()
        } else {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            bleForResult.launch(enableBtIntent)
            bluetoothAdapter.enable()
        }
    }

    /** 開始掃描藍牙設備 */
    private fun startScan() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter.isDiscovering || printService.state != PrinterServiceDelegate.State.NONE) return
        Log.d("MainActivity", "startDiscovery")

        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.startDiscovery()
        }
    }

    /** 停止掃描藍牙設備 */
    private fun stopScan() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isDiscovering) return
        Log.d("MainActivity", "cancelDiscovery")

        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        printService.disconnect()

        stopScan()

        try {
            unregisterReceiver(detectBluetoothStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            unregisterReceiver(detectBluetoothDeviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}