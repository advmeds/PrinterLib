package com.advmeds.printerlib.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class BPT3XPrinterService(private val usbManager: UsbManager) : UsbPrinterService(usbManager) {
    /** 是否支援該USB裝置 */
    public override fun isSupported(device: UsbDevice): Boolean =
        intArrayOf(1659, 1046, 7358, 1155, 8137, 1003, 11575, 1208).contains(device.vendorId) &&
                (0 until device.interfaceCount).map { device.getInterface(it) }.any { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER && it.interfaceSubclass == UsbConstants.USB_INTERFACE_SUBCLASS_BOOT }
}