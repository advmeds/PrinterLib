package com.advmeds.printerlib.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class BPT3XPrinterService(private val usbManager: UsbManager) : UsbPrinterService(usbManager) {

    /** 是否支援該USB裝置 */
    companion object {
        // 判斷 USB 設備是否支援的 Vendor ID 列表
        private val supportedVendorIds = intArrayOf(1659, 1046, 7358, 1155, 8137, 1003, 11575, 1208)

        private fun isSupport(device: UsbDevice): Boolean = supportedVendorIds.contains(device.vendorId) &&
                (0 until device.interfaceCount).map { device.getInterface(it) }.any { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER && it.interfaceSubclass == UsbConstants.USB_INTERFACE_SUBCLASS_BOOT }

        /** 是否支援該USB裝置 */
        fun isSupported(usbManager: UsbManager): UsbDevice? =
            usbManager.deviceList.values.find { isSupport(it) }
    }

    /** 是否支援該USB裝置 */
    override fun isSupported(device: UsbDevice): Boolean = isSupport(device)
}