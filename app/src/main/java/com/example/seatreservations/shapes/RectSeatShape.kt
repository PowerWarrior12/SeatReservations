package com.example.seatreservations.shapes

import android.graphics.*
import com.example.seatreservations.PlaceDisplay
import com.example.seatreservations.SeatReservationState
import com.example.seatreservations.drawText
import com.example.seatreservations.maxFrom
import kotlin.math.roundToInt

private const val DEFAULT_ITEM_SPACE_RATIO = 0.8f

class RectSeatShape(
    itemSize: Int,
    rowsTextPadding: Int,
    rowsSpacing: Int,
    width: Int,
    height: Int,
    itemBitmap: Bitmap,
    getPaintByState: (SeatReservationState) -> Paint?,
    var itemSpacing: Int,
    var lineSpacing: Int,
    var coreWidth: Int,
    var coreHeight: Int
): SeatShape(itemBitmap, width, height, itemSize, rowsTextPadding, rowsSpacing, getPaintByState) {

    private var rowsDisplay: Array<RowDisplay?> = emptyArray()

    override val calculateHeight: Int
        get() = (itemSize + itemSpacing) * map.size - lineSpacing + coreHeight

    override val calculateWidth: Int
        get() = (itemSize + lineSpacing) * map.maxFrom(0) { it.size } - itemSpacing + (rowsTextPadding + rowsSpacing) * 2

    override fun prepareDisplay() {
        var rowPosition = 1
        rowsDisplay = map.mapIndexed { rowIndex, array ->
            if (array.count { state -> state != SeatReservationState.EMPTY } > 0) {
                var placePosition = 1
                RowDisplay(rowPosition++, getYPositionByIndex(rowIndex), array.mapIndexed { placeIndex, state ->
                    if (state != SeatReservationState.EMPTY) {
                        PlaceDisplay(
                            state,
                            placePosition++,
                            itemSize,
                            getPositionByIndexes(Point(placeIndex, rowIndex)),
                            itemBitmap,
                            getPaintByState
                        )
                    } else {
                        null
                    }
                }.toTypedArray())
            } else {
                null
            }
        }.toTypedArray()
    }

    override fun updateDisplay() {
        rowsDisplay.forEachIndexed { rowIndex, rowDisplay ->
            rowDisplay?.let {
                it.updatePosition(getYPositionByIndex(rowIndex))
                it.updateRectangles(rowsTextPadding, rowsSpacing)
                it.placeDisplays.forEachIndexed { indexPlace, placeDisplay ->
                    placeDisplay?.updatePositionPoint(getPositionByIndexes(Point(indexPlace, rowIndex)))
                    placeDisplay?.updateRect()
                }
            }
        }
    }

    override fun recalculateParams(width: Int, height: Int): Boolean {

        fun recalculateItemSize(size: Int) {
            val count = Integer.max(map.size, map.maxFrom(0) { it.size })
            val calculateItemSize = size / (DEFAULT_ITEM_SPACE_RATIO * count + (1 - DEFAULT_ITEM_SPACE_RATIO) * (count - 1))
            itemSize = (calculateItemSize * DEFAULT_ITEM_SPACE_RATIO).roundToInt()
            itemSpacing = (calculateItemSize * (1 - DEFAULT_ITEM_SPACE_RATIO)).roundToInt()
            lineSpacing = itemSpacing
        }

        val widthByItems = calculateWidth
        val heightByItems = calculateHeight

        if (heightByItems != height && widthByItems != width) {
            recalculateItemSize(
                Integer.min(
                    width - (rowsTextPadding + rowsSpacing) * 2,
                    height - coreHeight
                )
            )
            return true
        }
        if (heightByItems != height) {
            recalculateItemSize(height - coreHeight)
            return true
        }
        if (widthByItems != width) {
            recalculateItemSize(width - (rowsTextPadding + rowsSpacing) * 2)
            return true
        }

        return false

    }

    override fun click(position: Point, onClick: Runnable?) {
        val indexes = getIndexesByPositionNormal(position)
        val rowDisplay = rowsDisplay.getOrNull(indexes.y)
        val placeDisplay = rowDisplay?.placeDisplays?.getOrNull(indexes.x)
        if (placeDisplay != null) {
            val newState = placeDisplay.click()
            map[indexes.y][indexes.x] = newState
            onItemClickListener?.onItemClick(newState, Pair(rowDisplay.getRowNumber(), placeDisplay.getSeatPosition()))
            onClick?.run()
        }
    }

    override fun draw(canvas: Canvas, rowTextPaint: Paint) {
        rowsDisplay.forEach { rowDisplay ->
            rowDisplay?.draw(canvas, rowTextPaint)
        }
    }

    private fun getIndexesByPositionNormal(position: Point): Point {
        return Point().apply {
            x = (position.x - rowsSpacing) / (itemSize + itemSpacing)
            y = (position.y - coreHeight) / (itemSize + lineSpacing)
        }
    }

    private fun getYPositionByIndex(index: Int) = coreHeight + index * (lineSpacing + itemSize)

    private fun getPositionByIndexes(indexes: Point): Point {
        return Point().apply {
            x = rowsSpacing + indexes.x * (itemSpacing + itemSize)
            y = getYPositionByIndex(indexes.y)
        }
    }

    /**
     * A class for visualizing a row
     */
    private inner class RowDisplay(
        private val rowNumber: Int,
        private var position: Int,
        val placeDisplays: Array<PlaceDisplay?>,
    ) {
        private val rectLeft = Rect()
        private val rectRight = Rect()

        init {
            updateRectangles(rowsTextPadding, rowsSpacing)
        }

        fun draw(canvas: Canvas, rowTextPaint: Paint) {
            canvas.apply {
                placeDisplays.forEach { placeDisplay ->
                    drawRaw(this, rowTextPaint)
                    placeDisplay?.draw(this, rowTextPaint)
                }
            }
        }

        fun getRowNumber(): Int {
            return rowNumber
        }

        fun updatePosition(newPosition: Int) {
            position = newPosition
        }

        fun updateRectangles(textPadding: Int, rowSpacing: Int) {
            rectLeft.apply {
                left = textPadding
                right = rowSpacing
                top = position
                bottom = position + itemSize
            }
            rectRight.apply {
                left = width - rowSpacing
                right = width - textPadding
                top = position
                bottom = position + itemSize
            }
        }

        private fun drawRaw(canvas: Canvas, rowTextPaint: Paint) {
            canvas.apply {
                drawText(rowNumber.toString(), rectLeft, rowTextPaint, Paint.Align.LEFT)
                drawText(rowNumber.toString(), rectRight, rowTextPaint, Paint.Align.RIGHT)
            }
        }
    }
}