package com.advmeds.printerlib.utils

import android.graphics.Bitmap
import net.posprinter.utils.BitmapToByteData

public object PrinterBuffer {
    public enum class CharacterSize(val rawValue: Byte) {
        XXSMALL(0x00.toByte()),
        XSMALL(0x11.toByte()),
        SMALL(0x22.toByte()),
        NORMAL(0x33.toByte()),
        LARGE(0x44.toByte()),
        XLARGE(0x55.toByte()),
        XXLARGE(0x66.toByte()),
        XXXLARGE(0x77.toByte()),
        XXXXLARGE(0x88.toByte());
    }

    /** 對齊方式 */
    public enum class Alignment(val rawValue: Byte) {
        /** 居左 */
        LEFT(0x00.toByte()),

        /** 居中 */
        CENTER(0x01.toByte()),

        /** 居右 */
        RIGHT(0x02.toByte());
    }

    /** 條碼對應字串的列印位置 */
    public enum class HRIAlignment(val rawValue: Byte) {
        /** 不列印HRI */
        NONE(0x00.toByte()),

        /** 在條碼上方 */
        TOP(0x01.toByte()),

        /** 在條碼下方 */
        BOTTOM(0x02.toByte()),

        /** 在條碼上方及下方 */
        BOTH(0x03.toByte());
    }

    /** 條碼類型 */
    public enum class BarCodeSystem(val rawValue: Byte) {
        UPC_A(0.toByte()),
        UPC_E(1.toByte()),
        EAN13(2.toByte()),
        EAN8(3.toByte()),
        CODE39(4.toByte()),
        I25(5.toByte()),
        CODEBAR(6.toByte()),
        CODE93(7.toByte()),
        CODE128(8.toByte())
    }

    public enum class BarCodeSystem2(val rawValue: Byte) {
        UPC_A(65.toByte()),
        UPC_E(66.toByte()),
        EAN13(67.toByte()),
        EAN8(68.toByte()),
        CODE39(69.toByte()),
        I25(70.toByte()),
        CODEBAR(71.toByte()),
        CODE93(72.toByte()),
        CODE128(73.toByte())
    }

    public enum class QRCodeErrorCorrectionLevel(val rawValue: Byte) {
        L(0x30.toByte()),
        M(0x31.toByte()),
        Q(0x32.toByte()),
        H(0x33.toByte());
    }

    fun horizontalPositioning(): ByteArray {
        return byteArrayOf(9)
    }

    fun printAndFeedLine(): ByteArray {
        return byteArrayOf(10)
    }

    fun printAndBackStandardModel(): ByteArray {
        return byteArrayOf(12)
    }

    fun printAndCarriageReturn(): ByteArray {
        return byteArrayOf(13)
    }

    fun cancelPrintDataByPageModel(): ByteArray {
        return byteArrayOf(24)
    }

    fun sendRealtimeStatus(n: Int): ByteArray {
        return byteArrayOf(16, 4, n.toByte())
    }

    fun requestRealtimeForPrint(n: Int): ByteArray {
        return byteArrayOf(16, 5, n.toByte())
    }

    fun openCashBoxRealtime(m: Int, t: Int): ByteArray {
        return byteArrayOf(16, 20, 1, m.toByte(), t.toByte())
    }

    fun printByPageModel(): ByteArray {
        return byteArrayOf(27, 12)
    }

    fun setCharRightSpace(n: Int): ByteArray {
        return byteArrayOf(27, 32, n.toByte())
    }

    fun selectPrintModel(n: Int): ByteArray {
        return byteArrayOf(27, 33, n.toByte())
    }

    fun setAbsolutePrintPosition(m: Int, n: Int): ByteArray {
        return byteArrayOf(27, 36, m.toByte(), n.toByte())
    }

    fun selectOrCancelCustomChar(n: Int): ByteArray {
        return byteArrayOf(27, 37, n.toByte())
    }

    fun defineUserDefinedCharacters(c1: Int, c2: Int, b: ByteArray): ByteArray {
        var data = byteArrayOf(27, 38, 3, c1.toByte(), c2.toByte())
        data = byteMerger(data, b)
        return data
    }

    fun selectBmpModel(m: Int, nL: Int, nH: Int, b: ByteArray): ByteArray {
        var data = byteArrayOf(27, 42, m.toByte(), nL.toByte(), nH.toByte())
        data = byteMerger(data, b)
        return data
    }

    fun selectOrCancelUnderlineModel(n: Int): ByteArray {
        return byteArrayOf(27, 45, n.toByte())
    }

    fun setDefaultLineSpacing(): ByteArray {
        return byteArrayOf(27, 50)
    }

    fun setLineSpacing(n: Int): ByteArray {
        return byteArrayOf(27, 51, n.toByte())
    }

    fun selectPrinter(n: Int): ByteArray {
        return byteArrayOf(27, 61, n.toByte())
    }

    fun cancelUserDefinedCharacters(n: Int): ByteArray {
        return byteArrayOf(27, 63, n.toByte())
    }

    fun initializePrinter(): ByteArray {
        return byteArrayOf(27, 64)
    }

    fun setHorizontalMovementPosition(b: ByteArray): ByteArray?{
        var data = byteArrayOf(27, 68)
        val nul = ByteArray(1)
        data = byteMerger(data, b)
        data = byteMerger(data, nul)
        return data
    }

    fun selectOrCancelBoldModel(n: Int): ByteArray {
        return byteArrayOf(27, 69, n.toByte())
    }

    fun selectOrCancelDoublePrintModel(n: Int): ByteArray {
        return byteArrayOf(27, 71, n.toByte())
    }

    fun printAndFeed(n: Int): ByteArray {
        return byteArrayOf(27, 74, n.toByte())
    }

    fun selectPageModel(): ByteArray {
        return byteArrayOf(27, 76)
    }

    fun selectFont(n: Int): ByteArray {
        return byteArrayOf(27, 77, n.toByte())
    }

    fun selectInternationalCharacterSets(n: Int): ByteArray {
        return byteArrayOf(27, 82, n.toByte())
    }

    fun selectStandardModel(): ByteArray {
        return byteArrayOf(27, 83)
    }

    fun selectPrintDirectionUnderPageModel(n: Int): ByteArray {
        return byteArrayOf(27, 84, n.toByte())
    }

    fun selectOrCancelCW90(n: Int): ByteArray {
        return byteArrayOf(27, 86, n.toByte())
    }

    fun setPrintAreaUnderPageModel(
        xL: Int,
        xH: Int,
        yL: Int,
        yH: Int,
        dxL: Int,
        dxH: Int,
        dyL: Int,
        dyH: Int,
    ): ByteArray {
        return byteArrayOf(
            27, 87,
            xL.toByte(),
            xH.toByte(),
            yL.toByte(), yH.toByte(), dxL.toByte(), dxH.toByte(), dyL.toByte(), dyH.toByte()
        )
    }

    fun setRelativeHorizontalPrintPosition(nL: Int, nH: Int): ByteArray {
        return byteArrayOf(27, 92, nL.toByte(), nH.toByte())
    }

    fun selectAlignment(n: Alignment): ByteArray {
        return byteArrayOf(27, 97, n.rawValue)
    }

    fun selectPrintTransducerOutPutPageOutSignal(n: Int): ByteArray {
        return byteArrayOf(27, 99, 51, n.toByte())
    }

    fun selectPrintTransducerStopPrint(n: Int): ByteArray {
        return byteArrayOf(27, 99, 52, n.toByte())
    }

    fun allowOrForbidPressButton(n: Int): ByteArray {
        return byteArrayOf(27, 99, 53, n.toByte())
    }

    fun printAndFeedForward(n: Int): ByteArray {
        return byteArrayOf(27, 100, n.toByte())
    }

    fun createCashBoxControlPulse(m: Int, t1: Int, t2: Int): ByteArray {
        return byteArrayOf(27, 112, m.toByte(), t1.toByte(), t2.toByte())
    }

    fun selectCharacterCodePage(n: Int): ByteArray {
        return byteArrayOf(27, 116, n.toByte())
    }

    fun selectOrCancelConvertPrintModel(n: Int): ByteArray {
        return byteArrayOf(27, 123, n.toByte())
    }

    fun selectCharacterSize(n: CharacterSize): ByteArray {
        return byteArrayOf(29, 33, n.rawValue)
    }

    fun setAbsolutePositionUnderPageModel(nL: Int, nH: Int): ByteArray {
        return byteArrayOf(29, 36, nL.toByte(), nH.toByte())
    }

    fun executePrintDataSaveByTransformToHex(): ByteArray {
        return byteArrayOf(29, 40, 65, 2, 0, 0, 1)
    }

    fun startOrStopMacroDefinition(): ByteArray {
        return byteArrayOf(29, 58)
    }

    fun selectOrCancelInvertPrintModel(n: Int): ByteArray {
        return byteArrayOf(29, 66, n.toByte())
    }

    fun selectHRICharacterPrintPosition(n: HRIAlignment): ByteArray {
        return byteArrayOf(29, 72, n.rawValue)
    }

    fun setLeftSpace(nL: Int, nH: Int): ByteArray {
        return byteArrayOf(29, 76, nL.toByte(), nH.toByte())
    }

    fun setHorizontalAndVerticalMoveUnit(x: Int, y: Int): ByteArray {
        return byteArrayOf(29, 80, x.toByte(), y.toByte())
    }

    fun selectCutPagerModerAndCutPager(m: Int): ByteArray {
        return byteArrayOf(29, 86, m.toByte())
    }

    fun selectCutPagerModerAndCutPager(m: Int, n: Int): ByteArray {
        return if (m != 66) {
            ByteArray(0)
        } else {
            byteArrayOf(29, 86, m.toByte(), n.toByte())
        }
    }

    fun setPrintAreaWidth(nL: Int, nH: Int): ByteArray {
        return byteArrayOf(29, 87, nL.toByte(), nH.toByte())
    }

    fun setVerticalRelativePositionUnderPageModel(
        nL: Int,
        nH: Int,
    ): ByteArray {
        return byteArrayOf(29, 92, nL.toByte(), nH.toByte())
    }

    fun executeMacroCommand(r: Int, t: Int, m: Int): ByteArray {
        return byteArrayOf(29, 94, r.toByte(), t.toByte(), m.toByte())
    }

    fun openOrCloseAutoReturnPrintState(n: Int): ByteArray {
        return byteArrayOf(29, 97, n.toByte())
    }

    fun selectHRIFont(n: Int): ByteArray {
        return byteArrayOf(29, 102, n.toByte())
    }

    fun setBarcodeHeight(n: Int): ByteArray {
        return byteArrayOf(29, 104, n.toByte())
    }

    fun printBarcode(m: BarCodeSystem, content: String): ByteArray {
        var data = byteArrayOf(29, 107, m.rawValue)
        val end = ByteArray(1)
        val text = content.toByteArray(charset("big5"))
        data = byteMerger(data, text)
        data = byteMerger(data, end)
        return data
    }

    fun printBarcode(m: BarCodeSystem2, n: Int, content: String): ByteArray {
        var data = byteArrayOf(29, 107, m.rawValue, n.toByte())
        val text = content.toByteArray(charset("big5"))
        data = byteMerger(data, text)
        return data
    }

    fun returnState(n: Int): ByteArray {
        return byteArrayOf(29, 114, n.toByte())
    }

    fun printRasterBmp(
        bitmap: Bitmap
    ): ByteArray {
        return BitmapToByteData.rasterBmpToSendData(
            0,
            bitmap,
            BitmapToByteData.BmpType.Threshold,
            BitmapToByteData.AlignType.Center,
            bitmap.width
        )
    }

    fun setBarcodeWidth(n: Int): ByteArray {
        return byteArrayOf(29, 119, n.toByte())
    }

    fun setChineseCharacterModel(n: Int): ByteArray {
        return byteArrayOf(28, 33, n.toByte())
    }

    fun selectChineseCharModel(): ByteArray {
        return byteArrayOf(28, 38)
    }

    fun selectOrCancelChineseCharUnderLineModel(n: Int): ByteArray {
        return byteArrayOf(28, 45, n.toByte())
    }

    fun cancelChineseCharModel(): ByteArray {
        return byteArrayOf(28, 46)
    }

    fun definedUserDefinedChineseChar(c2: Int, b: ByteArray): ByteArray {
        var data = byteArrayOf(28, 50, -2, c2.toByte())
        data = byteMerger(data, b)
        return data
    }

    fun setChineseCharLeftAndRightSpace(n1: Int, n2: Int): ByteArray {
        return byteArrayOf(28, 83, n1.toByte(), n2.toByte())
    }

    fun selectOrCancelChineseCharDoubleWH(n: Int): ByteArray {
        return byteArrayOf(28, 87, n.toByte())
    }

    fun printerOrderBuzzingHint(n: Int, t: Int): ByteArray {
        return byteArrayOf(27, 66, n.toByte(), t.toByte())
    }

    fun printerOrderBuzzingAndWarningLight(
        m: Int,
        t: Int,
        n: Int,
    ): ByteArray {
        return byteArrayOf(27, 67, m.toByte(), t.toByte(), n.toByte())
    }

    fun setsTheNumberOfColumnsOfTheDataAreaForPDF417(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 65, n.toByte())
    }

    fun setsTheNumberOfRowsOfTheDataAreaForPDF417(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 66, n.toByte())
    }

    fun setsTheModuleWidthOfPDF417(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 67, n.toByte())
    }

    fun setsTheModuleHeightForPDF417(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 68, n.toByte())
    }

    fun setsTheErrorCorrectionLevelForPDF417(m: Int, n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 4, 0, 48, 69, m.toByte(), n.toByte())
    }

    fun specifiesOrCancelsVariousPDF417SymbolOptions(m: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 70, m.toByte())
    }

    fun storesSymbolDataInThePDF417SymbolStorageArea(pL: Int, pH: Int, b: ByteArray): ByteArray {
        var data = byteArrayOf(
            29, 40, 107,
            pL.toByte(), pH.toByte(), 48, 80, 48
        )
        data = byteMerger(data, b)
        return data
    }

    fun printsThePDF417SymbolDataInTheSymbolStorageArea(): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 81, 48)
    }

    fun transmitsTheSizeOfTheSymbolDataInTheSymbolStorageAreaPDF417(): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 48, 82, 48)
    }

    fun setsTheSizeOfTheQRCodeSymbolModule(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 48, 103, n.toByte())
    }

    fun setsTheErrorCorrectionLevelForQRCodeSymbol(n: QRCodeErrorCorrectionLevel): ByteArray {
        return byteArrayOf(29, 40, 107, 48, 105, n.rawValue)
    }

    fun storesSymbolDataInTheQRCodeSymbolStorageArea(code: String): ByteArray {
        val b = code.toByteArray(charset("big5"))
        val a = b.size
        val pL: Int
        val pH: Int
        if (a <= 255) {
            pL = a
            pH = 0
        } else {
            pH = a / 256
            pL = a % 256
        }
        var data = byteArrayOf(29, 40, 107, 48, -128, pL.toByte(), pH.toByte())
        data = byteMerger(data, b)
        return data
    }

    fun printsTheQRCodeSymbolDataInTheSymbolStorageArea(): ByteArray {
        return byteArrayOf(29, 40, 107, 48, -127)
    }

    fun printQRCode(n: Int, errLevel: QRCodeErrorCorrectionLevel, code: String): ByteArray {
        val b = code.toByteArray(charset("big5"))
        val a = b.size
        val nL: Int
        val nH: Int
        if (a <= 255) {
            nL = a
            nH = 0
        } else {
            nH = a / 256
            nL = a % 256
        }
        var data = byteArrayOf(
            29, 40, 107, 48, 103,
            n.toByte(), 29, 40, 107, 48, 105,
            errLevel.rawValue, 29, 40, 107, 48, -128, nL.toByte(), nH.toByte()
        )
        data = byteMerger(data, b)
        val c = byteArrayOf(29, 40, 107, 48, -127)
        data = byteMerger(data, c)
        return data
    }

    fun transmitsTheSizeOfTheSymbolDataInTheSymbolStorageAreaQRCode(): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 49, 82, 48)
    }

    fun specifiesTheModeForMaxiCodeSymbol(n: Int): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 50, 65, n.toByte())
    }

    fun storesSymbolDataInTheMaxiCodeSymbolStorageArea(
        pL: Int,
        pH: Int,
        b: ByteArray,
    ): ByteArray {
        var data = byteArrayOf(
            29, 40, 107,
            pL.toByte(), pH.toByte(), 50, 80, 48
        )
        data = byteMerger(data, b)
        return data
    }

    fun printsTheMaxiCodeSymbolDataInTheSymbolStorageArea(): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 50, 81, 48)
    }

    fun transmitsTheSizeOfTheEncodedSymbolDataInTheSymbolStorageAreaMaxiCode(): ByteArray {
        return byteArrayOf(29, 40, 107, 3, 0, 50, 82, 48)
    }

    private fun byteMerger(byte1: ByteArray, byte2: ByteArray): ByteArray {
        val combine = ByteArray(byte1.size + byte2.size)
        System.arraycopy(byte1, 0, combine, 0, byte1.size)
        System.arraycopy(byte2, 0, combine, byte1.size, byte2.size)
        return combine
    }
}