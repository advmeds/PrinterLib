package com.advmeds.printerlib.usb

import android.hardware.usb.*

abstract class UsbPrinterService(private val usbManager: UsbManager) {
    /** 取得已接上USB裝置列表中第一個可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = usbManager.deviceList.values.find { isSupported(it) }

    /** 取得已連線的USB裝置 */
    public var connectedDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var epOut: UsbEndpoint? = null
    private var epIn: UsbEndpoint? = null
    private val isOpened: Boolean
        get() = usbConnection != null

    /**
     * 是否已連線
     *
     * NOTE：目前發現若拔出已經連線成功的設備但是未呼叫 disconnect()，則該變數仍然會回傳true。
     */
    public val isConnected: Boolean
        get() = isOpened && connectedDevice != null

    /** 是否支援該USB裝置 */
    public abstract fun isSupported(device: UsbDevice): Boolean

    /** 連線USB裝置 */
    public fun connectDevice(device: UsbDevice) {
        // 檢查當前是否已連線
        if (isOpened) {
            // 若已連線則在檢查已連線的裝置是否與準備要連線的裝置相同
            if (connectedDevice === device) {
                return
            }
            // 若不相同，則與已連線的裝置斷線
            disconnect()
        }

        if (!isSupported(device)) {
            throw IllegalArgumentException("The device is not supported.")
        }

        // 打開設備，獲取 UsbDeviceConnection 對象，連接設備，用於後面的通訊
        val connection = usbManager.openDevice(device) ?: throw IllegalArgumentException("Cannot open device.")

        // 尋找介面
        val usbInterface: UsbInterface? = (0.until(device.interfaceCount))
            .asSequence()
            .map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }

        val `interface` = usbInterface ?: throw IllegalArgumentException("Cannot find interface.")

        // 尋找終端接點
        val endpoints = 0.until(`interface`.endpointCount).map { `interface`.getEndpoint(it) }
        val epOut = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        val epIn = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }

        if (epOut == null || epIn == null) {
            connection.close()
            throw IllegalArgumentException("Cannot find endpoints.")
        }

        if (!connection.claimInterface(`interface`, true)) {
            connection.close()
            throw IllegalArgumentException("Cannot claim interface.")
        }

        this.connectedDevice = device
        this.usbConnection = connection
        this.usbInterface = `interface`
        this.epOut = epOut
        this.epIn = epIn
    }

    /** 斷線 */
    public fun disconnect() {
        usbConnection?.releaseInterface(usbInterface)
        usbConnection?.close()
        usbConnection = null
        usbInterface = null
        connectedDevice = null
        epOut = null
        epIn = null
    }

    public fun write(data: ByteArray) {
        val connection = requireNotNull(usbConnection) { "The printer is not opened." }

        val size = connection.bulkTransfer(epOut, data, data.size, 3000)

        require(size >= 0) { "The printer bulkTransfer failed!" }
    }

    public fun read(bufferSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
        val connection = requireNotNull(usbConnection) { "The printer is not opened." }

        val receiveBytes = ByteArray(bufferSize)
        val size = connection.bulkTransfer(epIn, receiveBytes, receiveBytes.size, 3000)

        require(size >= 0) { "The printer bulkTransfer failed!" }

        return receiveBytes.copyOf(size)
    }
}