package com.advmeds.printerlibdemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.advmeds.printerlib.BLEPrintService
import com.advmeds.printerlib.PrinterBuffer
import com.vise.baseble.ViseBle
import com.vise.baseble.callback.scan.IScanCallback
import com.vise.baseble.callback.scan.ScanCallback
import com.vise.baseble.model.BluetoothLeDevice
import com.vise.baseble.model.BluetoothLeDeviceStore
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val mainRecyclerView: RecyclerView by lazy { findViewById(R.id.main_rv) }
    private val mainAdapter = MainAdapter { position ->
        Log.d("onItemClick", position.toString())

        val device = devices[position]

        printService.connect(device.device)
    }
    private var devices = mutableListOf<BluetoothLeDevice>()

    private val scanCallback: ScanCallback by lazy {
        ScanCallback(object : IScanCallback {

            override fun onDeviceFound(bluetoothLeDevice: BluetoothLeDevice?) {
                bluetoothLeDevice?.name ?: return

                if (!devices.map { it.address }.contains(bluetoothLeDevice.address)) {
                    Log.d("IScanCallback", "onDeviceFound: $bluetoothLeDevice")
                    devices.add(bluetoothLeDevice)
                    mainAdapter.dataSet = devices.map { it.name }
                    mainAdapter.notifyDataSetChanged()
                }
            }

            override fun onScanFinish(bluetoothLeDeviceStore: BluetoothLeDeviceStore?) {

            }

            override fun onScanTimeout() {

            }
        })
    }

    private val printCallback = object : BLEPrintService.BLEPrintServiceDelegate {
        override fun onStateChanged(state: BLEPrintService.State) {
            Log.d("onStateChanged", state.toString())

            when (state) {
                BLEPrintService.State.NONE -> {

                }
                BLEPrintService.State.CONNECTING -> {

                }
                BLEPrintService.State.CONNECTED -> {
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
    private var printService: BLEPrintService = BLEPrintService(this, printCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecyclerView.adapter = mainAdapter
    }

    override fun onResume() {
        super.onResume()

        requestBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()

        ViseBle.getInstance().stopScan(scanCallback)
    }

    override fun onDestroy() {
        super.onDestroy()

        printService.disconnect()
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (EasyPermissions.hasPermissions(this, *permissions)) {
            startScan()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "${getString(R.string.app_name)}希望能夠啟用權限來連接藍牙設備",
                0,
                *permissions
            )
        }
    }

    // 開始掃描藍牙設備
    private fun startScan() {
        if (scanCallback.isScanning) return
        ViseBle.getInstance().startScan(scanCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        startScan()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }
}