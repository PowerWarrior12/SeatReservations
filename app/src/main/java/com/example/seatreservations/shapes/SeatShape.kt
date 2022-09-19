package com.example.seatreservations.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.example.seatreservations.OnItemClickListener
import com.example.seatreservations.SeatReservationState

abstract class SeatShape(
    private val seatShapeConfig: SeatShapeConfig
) {
    /**
     * Calculated height taking into account the dimensions of the parameters
     */
    abstract val calculateHeight: Int

    /**
     * Calculated width taking into account the dimensions of the parameters
     */
    abstract val calculateWidth: Int

    var map: Array<Array<SeatReservationState>> = emptyArray()

    /**
     * Calculated width taking into account the dimensions of the parameters
     */
    protected var onItemClickListener: OnItemClickListener? = null

    fun setOnClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun updateItemSize(newItemSize: Int) {
        seatShapeConfig.itemSize = newItemSize
    }
    fun updateWidth(newWidth: Int) {
        seatShapeConfig.width = newWidth
    }
    fun updateHeight(newHeight: Int) {
        seatShapeConfig.height = newHeight
    }
    fun updateItemSpacing(newItemSpacing: Int) {
        seatShapeConfig.itemSpacing = newItemSpacing
    }
    fun updateLineSpacing(newLineSpacing: Int) {
        seatShapeConfig.lineSpacing = newLineSpacing
    }

    fun getItemSize() = seatShapeConfig.itemSize
    fun getWidth() = seatShapeConfig.width
    fun getHeight() = seatShapeConfig.height
    fun getItemSpacing() = seatShapeConfig.itemSpacing
    fun getLineSpacing() = seatShapeConfig.lineSpacing
    fun getCoreHeight() = seatShapeConfig.coreHeight
    fun getCoreWidth() = seatShapeConfig.coreWidth
    fun getRowsTextPadding() = seatShapeConfig.rowsTextPadding
    fun getPaintByStyle() = seatShapeConfig.getPaintByState
    fun getRowsSpacing() = seatShapeConfig.rowsSpacing
    fun getItemBitmap() = seatShapeConfig.itemBitmap

    /**
     * Preparing the shape display
     */
    abstract fun prepareDisplay()

    /**
     * Updating the shape display when recalculating dimensions
     */
    abstract fun updateDisplay()

    /**
     * The value of item size and item spacing is recalculated, depending on the set view sizes.
     * They will be changed if the set dimensions of the view do not correspond to the calculations by the dimensions of the view elements.
     */
    abstract fun recalculateParams(width: Int, height: Int): Boolean

    /**
     * Processing of clicking on a shape
     */
    abstract fun click(position: Point, onClick: Runnable? = null)

    /**
     * Drawing a shape
     */
    abstract fun draw(canvas: Canvas, rowTextPaint: Paint)

    data class SeatShapeConfig(
        var itemBitmap: Bitmap, var width: Int, var height: Int, var itemSize: Int,
        var rowsTextPadding: Int,
        var rowsSpacing: Int,
        var getPaintByState: (SeatReservationState) -> Paint?,
        var lineSpacing: Int,
        var itemSpacing: Int,
        var coreHeight: Int,
        var coreWidth: Int
    )
}