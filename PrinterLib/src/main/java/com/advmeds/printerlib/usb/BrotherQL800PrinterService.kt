package com.advmeds.printerlib.usb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.widget.TextView
import com.brother.ptouch.sdk.connection.UsbConnection
import com.brother.sdk.lmprinter.Channel
import com.brother.sdk.lmprinter.OpenChannelError
import com.brother.sdk.lmprinter.PrintError.ErrorCode
import com.brother.sdk.lmprinter.PrinterDriver
import com.brother.sdk.lmprinter.PrinterDriverGenerator
import com.brother.sdk.lmprinter.PrinterModel
import com.brother.sdk.lmprinter.setting.QLPrintSettings
import com.google.zxing.BarcodeFormat
import com.google.zxing.oned.Code128Writer
import java.io.File
import java.io.FileOutputStream

class BrotherQL800PrinterService(private val context: Context) : UsbPrinterService(context.getSystemService(Context.USB_SERVICE) as UsbManager) {

    companion object {
        private fun isSupport(device: UsbDevice): Boolean = device.vendorId == 1273

        fun isSupported(usbManager: UsbManager): UsbDevice? =
            usbManager.deviceList.values.find { isSupport(it) }
    }

    private var driver: PrinterDriver? = null
    override val isOpened: Boolean
        get() = driver != null

    /** @see UsbConnection.isBrotherPrinter */
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

        require(isSupported(device)) {
            "The device is not supported."
        }

        val channel = Channel.newUsbChannel(context.getSystemService(Context.USB_SERVICE) as UsbManager)
        val driverResult = PrinterDriverGenerator.openChannel(channel)
        require(driverResult.error.code == OpenChannelError.ErrorCode.NoError) {
            driverResult.error.code.toString()
        }

        this.connectedDevice = device
        driver = driverResult.driver
    }

    override fun disconnect() {
        driver?.cancelPrinting()
        driver?.closeChannel()
        driver = null
    }

    /** 建構嘉義西區的個資貼紙，適用於 29mm * 90mm 紙卷 */
    fun printChiaYiInfo(
        name: String,
        idNo: String,
        mobile: String,
        birth: String,
        address: String
    ) {
        val driver = requireNotNull(driver) { "The printer is not opened." }

        // create a new document
        val document = PdfDocument()

        val textSize = 64
        val pageWidth = 580
        val pageHeight = 1800
        val top = -(pageHeight - pageWidth).toFloat()
        // crate a page description
        val pageInfo = PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // start a page
        val page = document.startPage(pageInfo).apply {
            canvas.rotate(-90f)
            canvas.translate(-pageWidth.toFloat(), 0f)
        }

        // Adding title
        TextView(context).apply {
            val titleSpannedString = SpannableStringBuilder().apply {
                val text = String.format("姓名 %s 身分證字號 %s 電話 %s", name, idNo, mobile)
                append(text)
                val nameIndex = text.indexOf(name)
                val idNoIndex = text.indexOf(idNo)
                val mobileIndex = text.indexOf(mobile)
                setSpan(UnderlineSpan(), nameIndex, nameIndex + name.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                setSpan(UnderlineSpan(), idNoIndex, idNoIndex + idNo.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                setSpan(UnderlineSpan(), mobileIndex, mobileIndex + mobile.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
            isSingleLine = true
            layout(0, 0, pageHeight, pageWidth)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            setTextColor(Color.BLACK)
            text = titleSpannedString
            isDrawingCacheEnabled = true
        }.run {
            page.canvas.drawBitmap(
                drawingCache,
                top,
                textSize.toFloat(),
                null
            )
        }

        // Adding body
        TextView(context).apply {
            val bodySpannedString = SpannableStringBuilder().apply {
                val text = String.format("出生年月日 %s 地址 %s", birth, address)
                append(text)
                val birthIndex = text.indexOf(birth)
                val addressIndex = text.indexOf(address)
                setSpan(UnderlineSpan(), birthIndex, birthIndex + birth.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                setSpan(UnderlineSpan(), addressIndex, addressIndex + address.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
            layout(0, 0, pageHeight, pageWidth)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            setTextColor(Color.BLACK)
            text = bodySpannedString
            isDrawingCacheEnabled = true
        }.run {
            page.canvas.drawBitmap(
                drawingCache,
                top,
                (textSize * 3).toFloat(),
                null
            )
        }

        val barcodeWidth = pageHeight / 4 * 3
        page.canvas.drawBitmap(
            createBarcodeBitmap(
                barcodeValue = idNo,
                widthPixels = barcodeWidth,
                heightPixels = textSize * 2
            ),
            top - (top / 2) + pageWidth / 2 - barcodeWidth / 2,
            (textSize * 6).toFloat(),
            null
        )

        val paint = Paint()
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 48f
        val textWidth = paint.measureText(idNo, 0, idNo.length)

        TextView(context).apply {
            isSingleLine = true
            layout(0, 0, textWidth.toInt(), pageWidth)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, paint.textSize)
            setTextColor(Color.BLACK)
            text = idNo
            isDrawingCacheEnabled = true
        }.run {
            page.canvas.drawBitmap(
                drawingCache,
                top - (top / 2) + pageWidth / 2 - textWidth / 2,
                (textSize * 8).toFloat(),
                null
            )
        }

        // finish the page
        document.finishPage(page)

        val filePath = File(context.cacheDir, "info.pdf")
        document.writeTo(FileOutputStream(filePath.absolutePath))

        // close the document
        document.close()

        val settings = QLPrintSettings(PrinterModel.QL_800).apply {
            workPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
            isAutoCut = true
            labelSize = QLPrintSettings.LabelSize.DieCutW29H90
        }
        val error = driver.printPDF(filePath.absolutePath, settings)
        require(error.code == ErrorCode.NoError) {
            error.errorDescription
        }
    }

    private fun createBarcodeBitmap(
        barcodeValue: String,
        widthPixels: Int,
        heightPixels: Int
    ): Bitmap {
        val bitMatrix = Code128Writer().encode(
            barcodeValue,
            BarcodeFormat.CODE_128,
            widthPixels,
            heightPixels
        )

        val pixels = IntArray(bitMatrix.width * bitMatrix.height)
        for (y in 0 until bitMatrix.height) {
            val offset = y * bitMatrix.width
            for (x in 0 until bitMatrix.width) {
                pixels[offset + x] =
                    if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(
            bitMatrix.width,
            bitMatrix.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.setPixels(
            pixels,
            0,
            bitMatrix.width,
            0,
            0,
            bitMatrix.width,
            bitMatrix.height
        )
        return bitmap
    }
}