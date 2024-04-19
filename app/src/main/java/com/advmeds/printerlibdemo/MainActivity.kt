package com.advmeds.printerlibdemo

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.advmeds.printerlib.bluetooth.BluetoothPrinterService
import com.advmeds.printerlib.bluetooth.PrinterServiceDelegate
import com.advmeds.printerlib.usb.BPT3XPrinterService
import com.advmeds.printerlib.usb.UsbPrinterService
import com.advmeds.printerlib.utils.PrinterBuffer

class MainActivity : AppCompatActivity() {
    companion object {
        private const val USB_PERMISSION = "${BuildConfig.APPLICATION_ID}.USB_PERMISSION"
    }
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

                    device?.name ?: return

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
                    val commandList = arrayListOf(
                        PrinterBuffer.initializePrinter(),
                        PrinterBuffer.selectAlignment(PrinterBuffer.Alignment.CENTER),
                        strToBytes("煩請親自依「掛號燈號」至櫃檯掛號。過號或號碼單遺失者，請重新抽號。"),
                        PrinterBuffer.printAndFeedLine(),
                        strToBytes(String.format("%04d", 69)),
                        PrinterBuffer.printAndFeedLine(),
                        PrinterBuffer.selectAlignment(PrinterBuffer.Alignment.CENTER),
                        PrinterBuffer.selectHRICharacterPrintPosition(PrinterBuffer.HRIAlignment.TOP),
                        PrinterBuffer.setBarcodeWidth(3),
                        PrinterBuffer.setBarcodeHeight(162),
                        PrinterBuffer.printBarcode(PrinterBuffer.BarCodeSystem2.CODE39, 10, "B12345789"),
                        PrinterBuffer.printAndFeedLine(),
                        PrinterBuffer.selectCutPagerModerAndCutPager(66, 1),
                    )

                    commandList.forEach {
                        printService.write(it)
                    }
                }
            }
        }
    }

    private lateinit var printService: BluetoothPrinterService

    private val detectUsbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return

            when (intent.action) {
                USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // user choose YES for your previously popup window asking for grant permission for this usb device
                        when (usbDevice.productId) {
                            usbPrinterService.supportedDevice?.productId -> {
                                try {
                                    usbPrinterService.connectDevice(usbDevice)
//                                    usbPrinterService.printPdf()

                                    val commandList = arrayListOf(
                                        PrinterBuffer.initializePrinter(),
                                        PrinterBuffer.selectAlignment(PrinterBuffer.Alignment.CENTER),
                                        PrinterBuffer.selectCharacterSize(PrinterBuffer.CharacterSize.XXSMALL),
                                        strToBytes("煩請親自依「掛號燈號」至櫃檯掛號。過號或號碼單遺失者，請重新抽號。"),
                                        PrinterBuffer.printAndFeedLine(),
                                        strToBytes(String.format("%04d", 69)),
                                        PrinterBuffer.printAndFeedLine(),
                                        PrinterBuffer.selectAlignment(PrinterBuffer.Alignment.CENTER),
                                        PrinterBuffer.selectHRICharacterPrintPosition(PrinterBuffer.HRIAlignment.BOTTOM),
                                        PrinterBuffer.setBarcodeWidth(3),
                                        PrinterBuffer.setBarcodeHeight(162),
                                        PrinterBuffer.printBarcode(PrinterBuffer.BarCodeSystem2.CODE39, 10, "B12345789"),
                                        PrinterBuffer.printAndFeedLine(),
                                        PrinterBuffer.selectCutPagerModerAndCutPager(66, 1),
                                    )

                                    commandList.forEach {
                                        usbPrinterService.write(it)
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        // user choose NO for your previously popup window asking for grant permission for this usb device
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    connectUSBDevice(usbDevice)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    when (usbDevice.productId) {
                        usbPrinterService.connectedDevice?.productId -> {
                            usbPrinterService.disconnect()
                        }
                    }
                }
            }
        }
    }

    private fun strToBytes(str: String): ByteArray =
        try {
            str.toByteArray(charset("big5"))
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf()
        }

    private lateinit var usbManager: UsbManager

    private lateinit var usbPrinterService: UsbPrinterService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        mainRecyclerView.adapter = mainAdapter
//
//        val bluetoothStateFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
//        registerReceiver(
//            detectBluetoothStateReceiver,
//            bluetoothStateFilter
//        )
//
//        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//        registerReceiver(detectBluetoothDeviceReceiver, filter)
//
//        requestBluetoothPermissions()
//
//        printService = BluetoothPrinterService(this, printCallback)

        setupUSB()
    }

    override fun onResume() {
        super.onResume()

//        requestBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()

//        stopScan()
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

    private fun setupUSB() {
        val usbFilter = IntentFilter(USB_PERMISSION)
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerReceiver(
            detectUsbDeviceReceiver,
            usbFilter
        )

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        usbPrinterService = BPT3XPrinterService(usbManager)
        usbPrinterService.supportedDevice?.also {
            connectUSBDevice(it)
        }
    }

    private fun connectUSBDevice(device: UsbDevice) {
        val mPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(USB_PERMISSION),
            0
        )

        usbManager.requestPermission(device, mPermissionIntent)
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