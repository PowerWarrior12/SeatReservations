package com.example.seatreservations

import android.graphics.*
import androidx.core.graphics.contains

/**
 * A class for visualizing a row, stores the
 * seat status, number, position, bitmap for rendering, and methods for rendering,
 * obtaining a position, processing a click and updating basic parameters
 */
class PlaceDisplay(
    private var state: SeatReservationState,
    private var seatPosition: Int,
    private var itemSize: Int,
    val positionPoint: Point,
    private val itemBitmap: Bitmap,
    private val getPaintByState: (SeatReservationState) -> Paint?
) {
    private val rect: Rect = Rect()

    init {
        updateRect()
    }

    fun draw(canvas: Canvas, selectedTextPaint: Paint) {
        drawPlace(canvas)
        drawSelectedText(canvas, selectedTextPaint)
    }

    fun drawPlace(canvas: Canvas) {
        canvas.drawBitmap(itemBitmap, null, rect, getPaintByState.invoke(state))
    }

    fun drawSelectedText(canvas: Canvas, selectedTextPaint: Paint) {
        if (state == SeatReservationState.SELECTED) {
            canvas.drawText(seatPosition.toString(), rect, selectedTextPaint, Paint.Align.CENTER)
        }
    }

    fun getSeatPosition(): Int {
        return seatPosition
    }

    fun click(): SeatReservationState {
        if (state == SeatReservationState.FREE) {
            state = SeatReservationState.SELECTED
        } else if (state == SeatReservationState.SELECTED) {
            state = SeatReservationState.FREE
        }
        return state
    }

    fun updatePositionPoint(newPositionPoint: Point) {
        positionPoint.apply {
            x = newPositionPoint.x
            y = newPositionPoint.y
        }
    }

    fun updateRect() {
        rect.apply {
            left = positionPoint.x
            right = positionPoint.x + itemSize
            top = positionPoint.y
            bottom = positionPoint.y + itemSize
        }
    }

    fun hasPosition(position: Point): Boolean = rect.contains(position)
}