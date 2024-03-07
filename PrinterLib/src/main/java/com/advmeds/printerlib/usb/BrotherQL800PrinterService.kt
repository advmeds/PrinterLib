package com.advmeds.printerlib.usb

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class BrotherQL800PrinterService(private val context: Context) : UsbPrinterService(context.getSystemService(Context.USB_SERVICE) as UsbManager) {
    private var driver: PrinterDriver? = null
    override val isOpened: Boolean
        get() = driver != null

    /** @see UsbConnection.isBrotherPrinter */
    override fun isSupported(device: UsbDevice): Boolean =
        device.vendorId == 1273

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

    fun printPdf() {
        val driver = requireNotNull(driver) { "The printer is not opened." }
        val pdfFile = buildPdf("test.pdf", "Title", "context")
        val settings = QLPrintSettings(PrinterModel.QL_800).apply {
            workPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
            isAutoCut = true
            labelSize = QLPrintSettings.LabelSize.RollW62RB
        }
        val error = driver.printPDF(pdfFile, settings)
        require(error.code == ErrorCode.NoError) {
            error.code.toString()
        }
//        val printerInfo = PrinterInfo().apply {
//            port = PrinterInfo.Port.USB
//            printerModel = PrinterInfo.Model.QL_800
//        }
//        val printer = Printer().apply {
//            this.printerInfo = printerInfo
//        }
//        val pdfFile = buildPdf("test.pdf", "Title", "context")
//        printer.startCommunication()
//        val result = printer.printPdfFile(pdfFile, 1)
//        printer.endCommunication()
    }

    private fun buildPdf(filename: String, headingText: String, bodyText: String): String? {
        // create a new document
        val document = PdfDocument()

        //TODO: Calculate the height based on number of lines fed in
        val headerTextSize = 30
        val bodyTextSize = 20

        //Calulate total pixels height by finding newlines
        var lastIndex = 0
        var count = 0
        while (lastIndex != -1) {
            lastIndex = bodyText.indexOf("\n", lastIndex)
            if (lastIndex != -1) {
                count++
                lastIndex += "\n".length
            }
        }
        val pageWidth = 234
        val pageHeight = headerTextSize * 2 + count * bodyTextSize + bodyTextSize

        // crate a page description
        val pageInfo = PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // start a page
        val page = document.startPage(pageInfo)

        // draw something on the page


        //TODO: Add a second textview and paint and everything

        //Adding title
        val textViewTitle = TextView(context)
        textViewTitle.layout(0, 0, pageWidth, pageHeight) //text box size heightpx x widthpx
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, headerTextSize.toFloat())
        textViewTitle.setTextColor(Color.BLACK)
        //textView.setShadowLayer(5, 2, 2, Color.CYAN); //text shadow
        textViewTitle.text = "$headingText\n_________________"
        textViewTitle.isDrawingCacheEnabled = true
        page.canvas.drawBitmap(textViewTitle.drawingCache, 1f, 1f, null)

        //Adding body
        val textView = TextView(context)
        textView.layout(0, 0, pageWidth, pageHeight) //text box size heightpx x widthpx
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodyTextSize.toFloat())
        textView.setTextColor(Color.BLACK)
        //textView.setShadowLayer(5, 2, 2, Color.CYAN); //text shadow
        textView.text = bodyText
        textView.isDrawingCacheEnabled = true
        page.canvas.drawBitmap(
            textView.drawingCache,
            1f,
            (headerTextSize + headerTextSize / 2).toFloat(),
            null
        )
        //text box top left position 50,50

//        canvas.save();
//        canvas.translate(50, 20); //position text on the canvas

        // finish the page
        document.finishPage(page)
        var filepath: String? = null
        try {
            val mypath = File(context.getExternalFilesDir(null), filename)
            filepath = mypath.absolutePath
            document.writeTo(FileOutputStream(mypath))
        } catch (e: FileNotFoundException) {
            Log.d("ez_UserAreaActivity", "File not found exception")
        } catch (e: IOException) {
            Log.d("ez_UserAreaActivity", "IOException")
        }

        // close the document
        document.close()
        return filepath
    }
}