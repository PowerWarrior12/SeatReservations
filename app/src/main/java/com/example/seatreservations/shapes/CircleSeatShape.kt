package com.example.seatreservations.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.example.seatreservations.*
import kotlin.math.*

private const val DEFAULT_ITEM_SPACE_RATIO = 0.8f

class CircleSeatShape(
    private val circleShapeConfig: SeatShapeConfig
) : SeatShape(circleShapeConfig) {

    override val calculateHeight: Int
        get() = getRowDistance(map.size - 1) + getCoreHeight() / 2

    override val calculateWidth: Int
        get() = (getRowDistance(map.size - 1) + getItemSize()) * 2

    private var rowsDisplay: Array<RowDisplay?> = emptyArray()

    override fun prepareDisplay() {
        var rowPosition = 1
        rowsDisplay = map.mapIndexed { rowIndex, array ->
            if (array.count { state -> state != SeatReservationState.EMPTY } > 0) {
                var placePosition = 1
                RowDisplay(rowPosition++, getRowDistance(rowIndex), array.mapIndexed { placeIndex, state ->
                    if (state != SeatReservationState.EMPTY) {
                        PlaceDisplay(
                            state,
                            placePosition++,
                            getItemSize(),
                            getPositionByIndexesCircle(
                                Point(placeIndex, rowIndex),
                                getAngleByIndex(placeIndex, array.size)
                            ),
                            getItemBitmap(),
                            getPaintByStyle()
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
                it.updateDistance(getRowDistance(rowIndex))
                it.placeDisplays.forEachIndexed { indexPlace, placeDisplay ->
                    placeDisplay?.updatePositionPoint(
                        getPositionByIndexesCircle(
                            Point(indexPlace, rowIndex),
                            getAngleByIndex(indexPlace, rowDisplay.placeDisplays.size)
                        )
                    )
                    placeDisplay?.updateRect()
                }
            }
        }
    }

    override fun recalculateParams(width: Int, height: Int): Boolean {

        fun recalculateItemSize(size: Int) {
            circleShapeConfig.apply {
                val count = map.size
                val calculateItemSize = size / count
                updateItemSize((calculateItemSize * DEFAULT_ITEM_SPACE_RATIO).roundToInt())
                updateItemSpacing((calculateItemSize * (1 - DEFAULT_ITEM_SPACE_RATIO)).roundToInt())

                val maxWidthCount = map.maxFrom(0) { it.size }
                val circleLength = 2 * PI * hypot(getCoreHeight().toFloat() / 2, getCoreWidth().toFloat() / 2) / 2
                //If the items dimensions do not fit into the smallest circle
                if (getItemSize() * (maxWidthCount - 1) > circleLength) {
                    updateItemSize((circleLength / maxWidthCount).toInt())
                    updateItemSpacing((getItemSize() / DEFAULT_ITEM_SPACE_RATIO * (1 * DEFAULT_ITEM_SPACE_RATIO)).toInt())

                }

                updateLineSpacing(getItemSpacing())
            }

        }

        circleShapeConfig.apply {

            val widthByItems = calculateWidth
            val heightByItems = calculateHeight
            val coreHypot = hypot(getCoreHeight().toFloat() / 2, getCoreWidth().toFloat() / 2).toInt()

            if (heightByItems != height && widthByItems != width) {
                recalculateItemSize(
                    Integer.min(
                        width / 2 - coreHypot,
                        height - coreHypot - getCoreHeight() / 2
                    )
                )
                return true
            }
            if (heightByItems != height) {
                recalculateItemSize(height - coreHypot - getCoreHeight() / 2)
                return true
            }
            if (widthByItems != width) {
                recalculateItemSize(width / 2 - coreHypot)
                return true
            }

            return false
        }
    }

    override fun click(position: Point, onClick: Runnable?) {
        val indexes = getIndexesByPosition(position) ?: return
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

    private fun getRowDistance(index: Int) = (hypot(
        getCoreHeight().toFloat() / 2,
        getCoreWidth().toFloat() / 2
    ) + (getItemSize() + getLineSpacing()) * index + getItemSize() / 2).toInt()

    private fun getIndexByRowDistance(distance: Int) = ((distance - hypot(
        getCoreHeight().toFloat() / 2,
        getCoreWidth().toFloat() / 2
    ) - getItemSize() / 2) / (getItemSize() + getLineSpacing())).roundToInt()

    private fun getPositionByIndexesCircle(indexes: Point, angle: Float): Point {
        val radian = degreeToRadian(angle)
        return Point().apply {
            x = getWidth() / 2 + (cos(radian) * getRowDistance(indexes.y)).toInt() - getItemSize() / 2
            y = getCoreHeight() / 2 + (sin(radian) * getRowDistance(indexes.y)).toInt() - getItemSize()
        }
    }

    private fun getAngleByIndex(index: Int, count: Int) = 180f / (count - 1) * index

    private fun getIndexByAngle(angle: Float, count: Int): Int {
        return (angle * (count - 1) / 180).roundToInt()
    }

    private fun getIndexesByPosition(position: Point): Point? {
        val point = Point((position.x - getWidth() / 2), -(position.y - getCoreHeight() / 2))
        val alpha = radianToDegree(degreeToPoint(point))
        val distance = distanceBtwPoints(position, Point(getWidth() / 2, getCoreHeight() / 2)).toInt()
        val rowNumber = getIndexByRowDistance(distance)
        if (rowNumber in map.indices) {
            return Point().apply {
                x = getIndexByAngle(alpha, map[rowNumber].size)
                y = rowNumber
            }
        }
        return null
    }

    /**
     * A class for visualizing a row
     */
    private inner class RowDisplay(
        private val rowNumber: Int,
        private var distance: Int,
        val placeDisplays: Array<PlaceDisplay?>,
    ) {

        fun draw(canvas: Canvas, rowTextPaint: Paint) {
            canvas.apply {
                placeDisplays.forEachIndexed { index, placeDisplay ->
                    placeDisplay?.let {
                        save()
                        val angle = getAngleByIndex(index, placeDisplays.size)
                        val positionForRotateX =
                            placeDisplay.getPositionPoint().x.toFloat() + getItemSize() / 2
                        val positionForRotateY =
                            placeDisplay.getPositionPoint().y.toFloat() + getItemSize() / 2
                        rotate(
                            angle,
                            positionForRotateX,
                            positionForRotateY
                        )
                        placeDisplay.drawPlace(this)
                        //-90 because need to rotate aside the root
                        rotate(
                            -90f,
                            positionForRotateX,
                            positionForRotateY
                        )
                        placeDisplay.drawSelectedText(this, rowTextPaint)
                        restore()
                    }
                }
            }
        }

        fun getRowNumber(): Int {
            return rowNumber
        }

        fun updateDistance(newDistance: Int) {
            distance = newDistance
        }
    }
}