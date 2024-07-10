package com.advmeds.printerlib.utils

import android.graphics.Bitmap
import com.csnprintersdk.csnio.CSNESCCmd
import com.csnprintersdk.csnio.CSNImageProcessing
import csnprint_compress_libs_api.CompressLib

object EP360CPrinterBuffer {

    private val cmd = CSNESCCmd()

    /** 文字對齊方式 */
    enum class Alignment(val value: Byte) {
        /** 居左 */
        LEFT(0.toByte()),

        /** 居中 */
        CENTER(1.toByte()),

        /** 居右 */
        RIGHT(2.toByte());
    }

    /** 字型風格 */
    enum class TextStyle(val value: Int) {
        /** 正常 */
        NORMAL(0x00),

        /** 粗體 */
        BOLD(0x08),

        /** 下底線 */
        UNDER_LINE(0x80),

        /** 加粗的下底線 */
        BOLD_UNDER_LINE(0x100),

        /** 顛倒180度,只在首行有效 */
        ROTATE180(0x200),

        /** 反顯黑底白字 */
        REVERSE_DISPLAY(0x400),

        /** 每個字符順時針旋轉90度 */
        ROTATE90(0x1000)
    }

    /** 字體類型 */
    enum class TextType(val value: Int) {
        /** 標準ASCII 12x24 */
        NORMAL(0x00),

        /** 壓縮ASCII 9x17 */
        ZIP(0x01)
    }

    /** BarCode 類型 */
    enum class BarCodeType(val value: Int) {
        /** UPC-A */
        UPC_A(0x41),

        /** UPC-C */
        UPC_C(0x42),

        /** JAN13(EAN13) */
        JAN13(0x43),

        /** JAC8(EAN8) */
        JAN8(0x44),

        /** CODE39 */
        CODE39(0x45),

        /** ITF */
        ITF(0x46),

        /** CODEBAR */
        CODEBAR(0x47),

        /** CODE93 */
        CODE93(0x48),

        /** CODE128 */
        CODE128(0x49)
    }

    /** BarCode 文字的類型 */
    enum class BarCodeFontType(val value: Int) {
        /** 標準ASCII */
        NORMAL(0x00),

        /** 壓縮ASCII */
        ZIP(0x01)
    }

    /** BarCode 文字打印的方式 */
    enum class BarCodeFontPosition(val value: Int) {
        /** 不列印 */
        NO_PRINT(0x00),

        /** 在條碼上方列印 */
        TOP(0x01),

        /** 在條碼下方列印 */
        BOTTOM(0x02),

        /** 在條碼上下都列印 */
        BOTH_PRINT(0x03)
    }

    enum class TextCharset(val value: Int) {
        /** GBK */
        GBK(0),

        /** UTF8 */
        UTF8(1),

        /** 保留 */
        NONE(2),

        /** BIG5 */
        BIG5(3),

        /** SHIFT-JIS */
        SHIFT_JIS(4),

        /** EUC-KR */
        EUC_KR(5)
    }

    /** 初始化 */
    fun initializePrinter() = cmd.ESC_ALT

    /** 空行 */
    fun feedLine() = cmd.CR + cmd.LF

    /**
     * 輸出文字
     * @param charset 字型編碼格式 0-GBK 1-UTF8 3-Bi5 4-Shift_JIS 5-EUC-KR
     * @param offsetX 文字的x偏移量 0-65535
     * @param textWidth 文字寬度 0-7
     * @param textHeight 文字高度 0-7
     * @param fontType 文字Type
     * @param fontStyle 文字風格
     * */
    fun textOut(
        pszString: String,
        charset: TextCharset = TextCharset.BIG5,
        offsetX: Int = 0,
        textWidth: Int = 0,
        textHeight: Int = 0,
        fontType: TextType = TextType.NORMAL,
        fontStyle: TextStyle = TextStyle.NORMAL
    ): ByteArray {

        require(pszString.isNotEmpty() && offsetX in 0..65535 && textWidth in 0..7 && textHeight in 0..7) { "textOut invalid args" }

        cmd.ESC_dollors_nL_nH[2] = (offsetX % 256).toByte()
        cmd.ESC_dollors_nL_nH[3] = (offsetX / 256).toByte()
        val intToWidth = byteArrayOf(0, 16, 32, 48, 64, 80, 96, 112)
        val intToHeight = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        cmd.GS_exclamationmark_n[2] = (intToWidth[textWidth] + intToHeight[textHeight]).toByte()
        var tmp_ESC_M_n = cmd.ESC_M_n
        if (fontType.value != 0 && fontType.value != 1) {
            tmp_ESC_M_n = byteArrayOf(0)
        } else {
            tmp_ESC_M_n[2] = fontType.value.toByte()
        }

        cmd.GS_E_n[2] = ((fontStyle.value shr 3) and 1).toByte()
        cmd.ESC_line_n[2] = ((fontStyle.value shr 7) and 3).toByte()
        cmd.FS_line_n[2] = ((fontStyle.value shr 7) and 3).toByte()
        cmd.ESC_lbracket_n[2] = ((fontStyle.value shr 9) and 1).toByte()
        cmd.GS_B_n[2] = ((fontStyle.value shr 10) and 1).toByte()
        cmd.ESC_V_n[2] = ((fontStyle.value shr 12) and 1).toByte()

        val pbString = when (charset) {
            TextCharset.GBK -> {
                cmd.ESC_9_n[2] = 0
                pszString.toByteArray(charset = charset("GBK"))
            }
            TextCharset.BIG5 -> {
                cmd.ESC_9_n[2] = 3
                pszString.toByteArray(charset = charset("Big5"))
            }
            TextCharset.SHIFT_JIS -> {
                cmd.ESC_9_n[2] = 4
                pszString.toByteArray(charset = charset("Shift_JIS"))
            }
            TextCharset.EUC_KR -> {
                cmd.ESC_9_n[2] = 5
                pszString.toByteArray(charset = charset("EUC-KR"))
            }
            else -> {
                cmd.ESC_9_n[2] = 1
                pszString.toByteArray()
            }
        }

        return cmd.ESC_dollors_nL_nH + cmd.GS_exclamationmark_n + tmp_ESC_M_n + cmd.GS_E_n + cmd.ESC_line_n + cmd.FS_line_n + cmd.ESC_lbracket_n + cmd.GS_B_n + cmd.ESC_V_n + cmd.FS_AND + cmd.ESC_9_n + pbString
    }

    /** 全切割紙張(只對帶全切切刀的機器有效) */
    fun fullCutPaper() = byteArrayOf(27, 105)

    /** 半切割紙張(只對帶半切切刀的機器有效) */
    fun halfCutPaper() = byteArrayOf(27, 109)

    /** 對齊方式 */
    fun align(align: Alignment): ByteArray {
        val data = cmd.ESC_a_n
        data[2] = align.value
        return data
    }

    /** 設定行高 */
    fun setLineHeight(height: Int): ByteArray {
        require(height in 0..255) { "setLineHeight invalid args" }

        val data = cmd.ESC_3_n
        data[2] = height.toByte()
        return data
    }

    /**
     * BarCode
     * @param strCodeData barcode 內容
     * @param offsetX x起始位置
     * @param type 條碼類型
     * @param width 條碼寬度
     * @param height 條碼高度 1-255
     * @param fontType 底下字體類型
     * @param fontPosition 底下字的位置
     *
     * 如果條碼太寬超出列印最大寬度的話,則不會列印,條碼格式有誤的話,也不會列印
     * */
    fun setBarCode(
        strCodeData: String,
        offsetX: Int,
        type: BarCodeType = BarCodeType.CODE128,
        width: Int = 2,
        height: Int = 50,
        fontType: BarCodeFontType = BarCodeFontType.NORMAL,
        fontPosition: BarCodeFontPosition = BarCodeFontPosition.BOTTOM
    ): ByteArray {
        require(offsetX in 0..65535 && type.value in 65..73 && height in 1..255) { "setBarCode invalid args" }

        val codeDataByeArray = strCodeData.toByteArray(charset("Big5"))
        cmd.ESC_dollors_nL_nH[2] = (offsetX % 256).toByte()
        cmd.ESC_dollors_nL_nH[3] = (offsetX / 256).toByte()
        cmd.GS_w_n[2] = width.toByte()
        cmd.GS_h_n[2] = height.toByte()
        cmd.GS_f_n[2] = (fontType.value and 1).toByte()
        cmd.GS_H_n[2] = (fontPosition.value and 3).toByte()
        cmd.GS_k_m_n_[2] = type.value.toByte()
        cmd.GS_k_m_n_[3] = codeDataByeArray.size.toByte()

        return cmd.ESC_dollors_nL_nH + cmd.GS_w_n + cmd.GS_h_n + cmd.GS_f_n + cmd.GS_H_n + cmd.GS_k_m_n_ + codeDataByeArray
    }

    /**
     * QR Code
     * @param strCodeData barcode 內容
     * @param moduleSize 二維碼每個模組的單元寬度 1-16
     * @param versionSize 二維碼的大小,設定0則為自動計算 0-16
     * @param errorCorrectionLevel 糾錯等級 1-4
     * */
    fun setQRCode(
        strCodeData: String,
        moduleSize: Int = 1,
        versionSize: Int = 0,
        errorCorrectionLevel: Int = 4
    ): ByteArray {
        require(moduleSize in 1..16 && errorCorrectionLevel in 1..4 && versionSize in 0..16) { "setQRCode invalid args" }

        val codeDataByeArray = strCodeData.toByteArray(charset("Big5"))

        cmd.GS_w_n[2] = moduleSize.toByte()
        cmd.GS_k_m_v_r_nL_nH[3] = versionSize.toByte()
        cmd.GS_k_m_v_r_nL_nH[4] = errorCorrectionLevel.toByte()
        cmd.GS_k_m_v_r_nL_nH[5] = (codeDataByeArray.size and 255).toByte()
        cmd.GS_k_m_v_r_nL_nH[6] = ((codeDataByeArray.size and '\uff00'.code) shr 8).toByte()

        return cmd.GS_w_n + cmd.GS_k_m_v_r_nL_nH + codeDataByeArray
    }

    /**
     * 列印兩個QR Code
     * @param qr1Data 第一個QR Code 資料
     * @param qr1Position x位置
     * @param qr1errorCorrectionLevel 糾錯等級 1-4
     * @param qr1Version 二維碼的大小,設定0則為自動計算 0-40
     * @param qr2Data 第二個QR Code 資料
     * @param qr2Position x位置
     * @param qr2errorCorrectionLevel 糾錯等級 1-4
     * @param qr2Version 二維碼的大小,設定0則為自動計算 0-40
     * @param moduleSize 二維碼每個模組的單元寬度 1-16
     * */
    fun setDoubleQRCode(
        qr1Data: String,
        qr1Position: Int,
        qr1errorCorrectionLevel: Int = 4,
        qr1Version: Int = 0,
        qr2Data: String,
        qr2Position: Int,
        qr2errorCorrectionLevel: Int = 4,
        qr2Version: Int = 0,
        moduleSize: Int = 1
    ): ByteArray {

        require(moduleSize in 1..16 && qr1errorCorrectionLevel in 1..4 && qr1Version in 0..40 && qr2errorCorrectionLevel in 1..4 && qr2Version in 0..40) { "setQRCode invalid args" }

        val head = byteArrayOf(31, 81, 2, moduleSize.toByte())
        val qr1CodeByteArray = qr1Data.toByteArray(charset = charset("Big5"))
        val qr1Info = byteArrayOf(
            ((qr1Position and '\uff00'.code) shr 8).toByte(),
            (qr1Position and 255).toByte(),
            ((qr1CodeByteArray.size and '\uff00'.code) shr 8).toByte(),
            (qr1CodeByteArray.size and 255).toByte(),
            qr1errorCorrectionLevel.toByte(),
            qr1Version.toByte()
        )

        val qr2CodeByteArray = qr2Data.toByteArray(charset = charset("Big5"))
        val qr2Info = byteArrayOf(
            ((qr2Position and '\uff00'.code) shr 8).toByte(),
            (qr2Position and 255).toByte(),
            ((qr2CodeByteArray.size and '\uff00'.code) shr 8).toByte(),
            (qr2CodeByteArray.size and 255).toByte(),
            qr2errorCorrectionLevel.toByte(),
            qr2Version.toByte()
        )

        return head + qr1Info + qr1CodeByteArray + qr2Info + qr2CodeByteArray
    }

    /**
     * 打印機的移動單位
     * @param horizontalMU 把水平方向上的移動單位設置 25.4 / horizontalMU 毫米
     * @param verticalMU 把垂直方向上的移動單位設置 25.4 / horizontalMU 毫米
     * */
    fun setMotionUnit(horizontalMU: Int, verticalMU: Int): ByteArray {
        require(horizontalMU in 0..255 && verticalMU in 0..255) { "setMotionUnit invalid args" }

        val data = cmd.GS_P_x_y
        data[2] = horizontalMU.toByte()
        data[3] = verticalMU.toByte()
        return data
    }

    /** 啟用多編碼,支持中日韓文 */
    fun setDoubleByteMode(): ByteArray = byteArrayOf(28, 38)

    /** 關閉多編碼, 使用純英文打印 */
    fun setSingleByteMode(): ByteArray = byteArrayOf(28, 46)

    /** 設定編碼 */
    fun setByteEncoding(charset: TextCharset): ByteArray = byteArrayOf(27, 57, charset.value.toByte())

    /**
     * 列印圖片
     * @param bitmap 需要列印的圖
     * @param width 圖的寬度,如果width和bitmap的寬度不一致,會等比縮放到 width的寬
     * 2"打印機(58mm) 最大寬度不超過384
     * 3"打印機(80mm) 最大寬度不超過576
     * @param binaryAlgorithm 二值化算法 0-使用抖動算法,對彩色圖片有較好的效果 1-平均閥值算法,對文本類圖片有較好的效果
     * @param compressMethod 壓縮算法 0-不使用壓縮算法 1-使用壓縮算法
     * */
    fun printPicture(bitmap: Bitmap, width: Int = 576, binaryAlgorithm: Int = 1, compressMethod: Int = 0): ByteArray {
        var mBitmap = bitmap
        val dstw: Int = (width + 7) / 8 * 8
        val dsth: Int = mBitmap.height * dstw / mBitmap.width
        val dst = IntArray(dstw * dsth)
        mBitmap = CSNImageProcessing.resizeImage(bitmap, dstw, dsth)
        mBitmap.getPixels(dst, 0, dstw, 0, 0, dstw, dsth)
        val gray = CSNImageProcessing.GrayImage(dst)
        val dithered = BooleanArray(dstw * dsth)
        if (binaryAlgorithm == 0) {
            CSNImageProcessing.format_K_dither16x16(dstw, dsth, gray, dithered)
        } else {
            CSNImageProcessing.format_K_threshold(dstw, dsth, gray, dithered)
        }

        val data = when (compressMethod) {
            1 -> CSNImageProcessing.eachLinePixToCompressCmd(dithered, dstw)
            2 -> CompressLib.RasterImageToCompressCmd(
                dstw,
                dsth,
                CSNImageProcessing.Image1ToRasterData(dstw, dsth, dithered)
            )

            else -> CSNImageProcessing.eachLinePixToCmd(dithered, dstw, 0)
        }

        return data
    }
}