package com.example.seatreservations.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.example.seatreservations.*
import kotlin.math.*

private const val DEFAULT_ITEM_SPACE_RATIO = 0.8f

class CircleSeatShape(
    itemBitmap: Bitmap,
    itemSize: Int,
    width: Int,
    height: Int,
    rowTextSpacing: Int,
    rowTextPadding: Int,
    getPaintByState: (SeatReservationState) -> Paint?,
    var coreHeight: Int,
    var coreWidth: Int,
    var lineSpacing: Int,
    var itemSpacing: Int,
) : SeatShape(itemBitmap, width, height, itemSize, rowTextPadding, rowTextSpacing, getPaintByState) {

    override val calculateHeight: Int
        get() = getRowDistance(map.size - 1) + coreHeight / 2

    override val calculateWidth: Int
        get() = (getRowDistance(map.size - 1) + itemSize) * 2

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
                            itemSize,
                            getPositionByIndexesCircle(
                                Point(placeIndex, rowIndex),
                                getAngleByIndex(placeIndex, array.size)
                            ),
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
            val count = map.size
            val calculateItemSize = size / count
            itemSize = (calculateItemSize * DEFAULT_ITEM_SPACE_RATIO).roundToInt()
            itemSpacing = (calculateItemSize * (1 - DEFAULT_ITEM_SPACE_RATIO)).roundToInt()

            val maxWidthCount = map.maxFrom(0) { it.size }
            val circleLength = 2 * PI * hypot(coreHeight.toFloat()/2, coreWidth.toFloat()/2) / 2
            //If the items dimensions do not fit into the smallest circle
            if (itemSize * (maxWidthCount - 1) > circleLength) {
                itemSize = (circleLength / maxWidthCount).toInt()
                itemSpacing = (itemSize / DEFAULT_ITEM_SPACE_RATIO * (1 * DEFAULT_ITEM_SPACE_RATIO)).toInt()
            }

            lineSpacing = itemSpacing
        }

        val widthByItems = calculateWidth
        val heightByItems = calculateHeight
        val coreHypot = hypot(coreHeight.toFloat()/2, coreWidth.toFloat()/2).toInt()

        if (heightByItems != height && widthByItems != width) {
            recalculateItemSize(
                Integer.min(
                    width/2 - coreHypot,
                    height - coreHypot - coreHeight/2
                )
            )
            return true
        }
        if (heightByItems != height) {
            recalculateItemSize(height - coreHypot - coreHeight/2)
            return true
        }
        if (widthByItems != width) {
            recalculateItemSize(width/2 - coreHypot)
            return true
        }

        return false
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

    private fun getRowDistance(index: Int) = (hypot(coreHeight.toFloat()/2, coreWidth.toFloat()/2) + (itemSize + lineSpacing) * index + itemSize / 2).toInt()

    private fun getIndexByRowDistance(distance: Int) = ((distance - hypot(coreHeight.toFloat()/2, coreWidth.toFloat()/2) - itemSize / 2) / (itemSize + lineSpacing)).roundToInt()

    private fun getPositionByIndexesCircle(indexes: Point, angle: Float): Point {
        val radian = degreeToRadian(angle)
        return Point().apply {
            x = width / 2 + (cos(radian) * getRowDistance(indexes.y)).toInt() - itemSize/2
            y = coreHeight / 2 + (sin(radian) * getRowDistance(indexes.y)).toInt() - itemSize
        }
    }

    private fun getAngleByIndex(index: Int, count: Int) = 180f / (count - 1) * index

    private fun getIndexByAngle(angle: Float, count: Int): Int {
        return (angle * (count - 1) / 180).roundToInt()
    }

    private fun getIndexesByPosition(position: Point): Point? {
        val point = Point((position.x - width/2 ), -(position.y - coreHeight/2))
        val alpha = radianToDegree(degreeToPoint(point))
        val distance = distanceBtwPoints(position, Point(width/2, coreHeight/2)).toInt()
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
                drawRaw(this, rowTextPaint)
                placeDisplays.forEachIndexed { index, placeDisplay ->
                    placeDisplay?.let {
                        save()
                        val angle = getAngleByIndex(index, placeDisplays.size)
                        val positionForRotateX = placeDisplay.positionPoint.x.toFloat() + itemSize / 2
                        val positionForRotateY = placeDisplay.positionPoint.y.toFloat() + itemSize / 2
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

        private fun drawRaw(canvas: Canvas, rowTextPaint: Paint) {
        }
    }

}