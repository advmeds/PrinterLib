package com.advmeds.printerlib.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.csnprintersdk.csnio.CSNPOS
import com.csnprintersdk.csnio.CSNUSBPrinting
import com.csnprintersdk.csnio.csnbase.CSNIOCallBack

class EP360CPrintService(private val context: Context) : UsbPrinterService(context.getSystemService(
    Context.USB_SERVICE) as UsbManager
) {

    companion object {

        private fun isSupport(device: UsbDevice): Boolean = device.vendorId == 4070

        /** 是否支援該USB裝置 */
        fun isSupported(usbManager: UsbManager): UsbDevice? =
            usbManager.deviceList.values.find { isSupport(it) }
    }

    private val mPos = CSNPOS()
    private val mUsb = CSNUSBPrinting()

    override val isOpened: Boolean
        get() = mUsb.IsOpened()

    /** 是否支援該USB裝置 */
    override fun isSupported(device: UsbDevice): Boolean = isSupport(device)

    override fun connectDevice(device: UsbDevice) {
        // 檢查當前是否已連線
        if (isOpened) {
            // 若已連線則在檢查已連線的裝置是否與準備要連線的裝置相同
            if (connectedDevice === device) {
                return
            }
            // 若不相同，則與已連線的裝置斷線
            disconnect()
        }

        require(isSupported(device)) { "The device is not supported." }

        mPos.Set(mUsb)
        mUsb.SetCallBack(object : CSNIOCallBack{
            override fun OnOpen() {}

            override fun OnOpenFailed() {}

            override fun OnClose() {}
        })

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        mUsb.Open(usbManager, device, context)

        this.connectedDevice = device
    }

    override fun write(data: ByteArray) {

        require(mUsb.IsOpened()){ "The printer is not opened. " }

        mUsb.mMainLocker.lock()
        try {
            mUsb.Write(data, 0 , data.size)
        } catch (e: Exception){
            e.printStackTrace()
        } finally {
            mUsb.mMainLocker.unlock()
        }

    }
}