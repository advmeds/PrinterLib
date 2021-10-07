package com.advmeds.printerlib

import android.graphics.Bitmap
import java.nio.charset.Charset

public class PrinterBuffer {
    /** 對齊方式 */
    public enum class TextAlignment(val rawValue: Byte) {
        /** 居左 */
        LEFT(0x00.toByte()),

        /** 居中 */
        CENTER(0x01.toByte()),

        /** 居右 */
        RIGHT(0x02.toByte());
    }

    /** 字體大小 */
    public enum class FontSize(val rawValue: Byte) {
        SMALL(0x00.toByte()),
        MIDDLE(0x11.toByte()),
        BIG(0x22.toByte());
    }

    /** 條碼對應字串的列印位置 */
    public enum class HRIAlignment(val rawValue: Byte) {
        /** 不列印HRI */
        NONE(0x00.toByte()),

        /** 在條碼下方 */
        BOTTOM(0x01.toByte()),

        /** 在條碼上方 */
        TOP(0x02.toByte()),

        /** 在條碼上方及下方 */
        BOTH(0x03.toByte());
    }

    /** 條碼類型 */
    public enum class BarCodeType(val rawValue: Byte) {
        UPC_A(0x00.toByte()),
        UPC_E(0x01.toByte()),
        EAN13(0x02.toByte()),
        EAN8(0x03.toByte()),
        CODE39(0x04.toByte()),
        I25(0x05.toByte()),
        CODEBAR(0x06.toByte()),
        CODE93(0x07.toByte()),
        CODE128(0x08.toByte()),
        CODE11(0x09.toByte()),
        MSI(0x10.toByte());
    }

    private var _dataBuffer = byteArrayOf()

    public val data: ByteArray
        get() = _dataBuffer

    init {
        // 1. 初始化印表機
        val initBytes = byteArrayOf(0x1B.toByte(), 0x40.toByte())
        _dataBuffer += initBytes

        // 2. 設置行距為1/6英寸
        // 另一種設置行距的方法為
        val lineSpace = byteArrayOf(0x1B.toByte(), 0x32.toByte())
        _dataBuffer += (lineSpace)

        // 3. 設置字體：標準為0x00，壓縮為0x01
        val fontBytes = byteArrayOf(0x1B.toByte(), 0x4D.toByte(), 0x00.toByte())
        _dataBuffer += (fontBytes)
    }

    // region 基本操作
    /** 換行 */
    public fun appendNewLine() {
        _dataBuffer += (0x0A.toByte())
    }

    /** 回車 */
    public fun appendReturn() {
        _dataBuffer += (0x0D.toByte())
    }

    /**
     * 設置對齊方式
     *
     * @param alignment 對齊方式：居左、居中、居右
     */
    private fun setAlignment(alignment: TextAlignment) {
        _dataBuffer += byteArrayOf(0x1B.toByte(), 0x61.toByte(), alignment.rawValue)
    }

    /**
     * 設置字體大小
     *
     * @param fontSize 字號
     */
    private fun setFontSize(fontSize: FontSize) {
        _dataBuffer += byteArrayOf(0x1D.toByte(), 0x21.toByte(), fontSize.rawValue)
    }

    /**
     * 添加文字，不換行
     *
     * @param text 文字內容
     */
    private fun setText(text: String) {
        _dataBuffer += text.toByteArray(Charset.forName("GBK"))
    }

    /**
     * 設置條碼對應字串(HRI)的列印位置
     *
     * @param alignment 列印方式：不列印、上方、下方、全都要
     */
    private fun setBarCodeHRIAlignment(alignment: HRIAlignment) {
        _dataBuffer += byteArrayOf(0x1D.toByte(), 0x48.toByte(), alignment.rawValue)
    }

    /**
     * 設置條碼高度
     *
     * @param height 1 <= size <= 255
     */
    private fun setBarCodeHeight(height: Int) {
        require(height in 1..255)
        _dataBuffer += byteArrayOf(0x1D.toByte(), 0x68.toByte(), height.toByte())
    }

    /**
     * 設置條碼寬度
     *
     * @param width 2 <= size <= 3
     */
    private fun setBarCodeWidth(width: Int) {
        require(width in 2..3)
        _dataBuffer += byteArrayOf(0x1D.toByte(), 0x77.toByte(), width.toByte())
    }

    /**
     * 列印條碼
     *
     * @param info 條碼資料
     * @param type 條碼類型
     */
    private fun setBarCodeInfo(
        info: String,
        type: BarCodeType
    ) {
        val barCodeData = byteArrayOf(
            0x1D.toByte(),
            0x6B.toByte(),
            type.rawValue
        ).plus(info.map { it.code.toByte() })
            .plus(0x00.toByte())
        _dataBuffer += barCodeData
    }

    /**
     * 設置QRCode大小
     *
     * @param size 1 <= size <= 16，QRCode的寬高相等
     */
    private fun setQRCodeSize(size: Int) {
        require(size in 1..16)
        _dataBuffer += byteArrayOf(
            0x1D.toByte(),
            0x28.toByte(),
            0x6B.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0x31.toByte(),
            0x43.toByte(),
            size.toByte()
        )
    }

    /**
     * 設置QRCode糾錯等級
     *
     * @param level 48 <= level <= 51
     */
    private fun setQRCodeErrorCorrection(level: Int) {
        require(level in 48..51)
        _dataBuffer += byteArrayOf(
            0x1D.toByte(),
            0x28.toByte(),
            0x6B.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0x31.toByte(),
            0x45.toByte(),
            level.toByte()
        )
    }

    /**
     * 將QRCode資料儲存至符號存儲區
     * [範圍]：4≤(pL+pHx256)≤7092 (0≤pL≤255,0≤pH≤27)
     * cn=49
     * fn=80
     * m=48
     * k=(pL+pH×256)-3, k就是資料的長度
     *
     * @param info QRCode的資料
     */
    private fun setQRCodeInfo(info: String) {
        val kLength = info.length + 3
        val pL = kLength % 256
        val pH = kLength / 256

        val dataBytes = byteArrayOf(
            0x1D.toByte(),
            0x28.toByte(),
            0x6B.toByte(),
            pL.toByte(),
            pH.toByte(),
            0x31.toByte(),
            0x50.toByte(),
            48.toByte()
        )
        _dataBuffer += dataBytes
        val infoData = info.toByteArray(Charsets.UTF_8)
        _dataBuffer += infoData
    }

    /** 印出之前儲存的QRCode資料 */
    private fun printStoredQRData() {
        _dataBuffer += byteArrayOf(
            0x1D.toByte(),
            0x28.toByte(),
            0x6B.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0x31.toByte(),
            0x51.toByte(),
            48.toByte()
        )
    }
    // endregion

    // region function method
    public fun appendText(
        text: String,
        alignment: TextAlignment,
        fontSize: FontSize = FontSize.SMALL
    ) {
        // 1. 文字對齊方式
        setAlignment(alignment)

        // 2. 設置字號
        setFontSize(fontSize)

        // 3. 設置標題內容
        setText(text)

        // 4. 換行
        appendNewLine()

        if (fontSize != FontSize.SMALL) {
            appendNewLine()
        }
    }

    // region 圖片
    public fun appendImage(
        image: Bitmap,
        alignment: TextAlignment,
        maxWidth: Int
    ) {
        // 1. 設置圖片對齊方式
        setAlignment(alignment)

    }

    public fun appendQRCodeWithInfo(
        info: String,
        size: Int,
        alignment: TextAlignment = TextAlignment.CENTER
    ) {
        setAlignment(alignment)
        setQRCodeSize(size)
        setQRCodeErrorCorrection(48)
        setQRCodeInfo(info)
        printStoredQRData()
        appendNewLine()
    }

    @Deprecated("待完成")
    public fun appendBarCodeWithInfo(
        info: String,
        height: Int = 50,
        width: Int = 3,
        alignment: TextAlignment = TextAlignment.CENTER,
        hriAlignment: HRIAlignment = HRIAlignment.BOTTOM
    ) {
//        setAlignment(alignment)
        setBarCodeHRIAlignment(hriAlignment)
//        setBarCodeHeight(height)
//        setBarCodeWidth(width)
        setBarCodeInfo(info, BarCodeType.UPC_A)
    }
    // endregion

    /**
     * 添加自定義的Data
     *
     * @param bytes 自定義的Data
     */
    public fun appendBytes(bytes: ByteArray) {
        _dataBuffer += bytes
    }
    // endregion

    // region 其它
    public fun appendDivider() {
        setAlignment(TextAlignment.CENTER)

        setFontSize(FontSize.SMALL)

        val line = "- - - - - - - - - - - - - - - -"
        setText(line)

        appendNewLine()
    }
    // endregion
}