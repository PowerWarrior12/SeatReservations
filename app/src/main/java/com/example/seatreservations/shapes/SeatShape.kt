package com.example.seatreservations.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.example.seatreservations.OnItemClickListener
import com.example.seatreservations.SeatReservationState

abstract class SeatShape(
    var itemBitmap: Bitmap, var width: Int, var height: Int, var itemSize: Int,
    var rowsTextPadding: Int,
    var rowsSpacing: Int,
    var getPaintByState: (SeatReservationState) -> Paint?
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
}