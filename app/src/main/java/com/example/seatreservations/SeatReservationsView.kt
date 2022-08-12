package com.example.seatreservations

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.graphics.contains
import com.example.seatreservations.SeatReservationsView.SeatReservationState
import com.example.seatreservations.SeatReservationsView.SeatReservationState.*
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.roundToInt

private const val MEASURED_ERROR = "Error with measured"
private const val DEFAULT_ITEM_SPACE_RATIO = 0.8f
private const val DEFAULT_ITEM_SELECTED_TEXT_RATIO = 0.8f
private const val DEFAULT_ITEM_ROW_TEXT_RATIO = 0.6f

/**
 * View for visualization and selection of seats. To display your hall, you need to call the method [updateMap]
 * ,in which you need to pass a two-dimensional array representing the states of [SeatReservationState]:
 * ### EMPTY:
 * to denote emptiness
 * ### FREE
 * to indicate a free seat
 * ### BOOKED
 * to indicate a place booked by someone else
 * ### SELECTED
 * to indicate the location selected by the user
 * ## Size calculation
 * Custom dimensions of seats, spaces between rows and seats, text to indicate the seat
 * number will be applied only if the wrap_content mode for height
 * and width is selected, otherwise they will be calculated automatically according to the specified ratios
 */
class SeatReservationsView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    @ColorInt
    private var selectedColor: Int = DEFAULT_SELECTED_COLOR

    @ColorInt
    private var freeColor: Int = DEFAULT_FREE_COLOR

    @ColorInt
    private var bookedColor: Int = DEFAULT_BOOKED_COLOR

    @ColorInt
    private var selectedTextColor: Int = DEFAULT_SELECTED_TEXT_COLOR

    @ColorInt
    private var rowTextColor: Int = DEFAULT_ROW_TEXT_COLOR

    @Px
    private var itemSpacing: Int = DEFAULT_ITEM_SPACING

    @Px
    private var lineSpacing: Int = DEFAULT_LINE_SPACING

    @Px
    private var sceneSpacing: Int = DEFAULT_SCENE_SPACING

    @Px
    private var itemSize: Int = DEFAULT_ITEM_SIZE

    @Px
    private var _sceneWidth: Int = DEFAULT_SCENE_WIDTH

    @Px
    private var sceneWidth: Int = DEFAULT_SCENE_WIDTH

    @Px
    private var sceneHeight: Int = DEFAULT_SCENE_HEIGHT

    @Px
    private var selectedTextSize: Int = DEFAULT_SELECTED_TEXT_SIZE

    @Px
    private var rowTextSize: Int = DEFAULT_ROW_TEXT_SIZE

    @Px
    private var rowsSpacing: Int = DEFAULT_ROWS_SPACING

    @Px
    private var rowsTextPadding: Int = DEFAULT_ROW_TEXT_PADDING

    @DrawableRes
    private var sceneDrawable: Int = DEFAULT_SCENE_DRAWABLE

    @DrawableRes
    private var itemDrawable: Int = DEFAULT_ITEM_DRAWABLE

    private var selectedFontReference: Int = DEFAULT_SELECTED_TEXT_FONT
    private var rowFontReference: Int = DEFAULT_ROW_TEXT_FONT

    private var onItemClickListener: OnItemClickListener? = null

    private val hallPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val freePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bookedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var itemBitmap: Bitmap
    private var sceneBitmap: Bitmap

    private var map: Array<Array<SeatReservationState>> = TEST_MAP

    private var rowsDisplay: Array<RowDisplay?> = emptyArray()

    private val calculateViewHeightByItems
        get() = (itemSize + itemSpacing) * map.size - lineSpacing + sceneHeight + sceneSpacing

    private val calculateViewWidthByItems
        get() = (itemSize + lineSpacing) * map.maxFrom(0) { it.size } - itemSpacing + (rowsTextPadding + rowsSpacing) * 2

    private val sceneRect: Rect = Rect()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.seat_reservation).apply {
            try {
                selectedColor = getColor(R.styleable.seat_reservation_selected_color, DEFAULT_SELECTED_COLOR)
                freeColor = getColor(R.styleable.seat_reservation_free_color, DEFAULT_FREE_COLOR)
                selectedTextColor =
                    getColor(R.styleable.seat_reservation_selected_text_color, DEFAULT_SELECTED_TEXT_COLOR)
                rowTextColor = getColor(R.styleable.seat_reservation_row_text_color, DEFAULT_ROW_TEXT_COLOR)
                bookedColor = getColor(R.styleable.seat_reservation_booked_color, DEFAULT_BOOKED_COLOR)

                itemSpacing = getDimensionPixelOffset(R.styleable.seat_reservation_item_spacing, DEFAULT_ITEM_SPACING)
                lineSpacing = getDimensionPixelOffset(R.styleable.seat_reservation_line_spacing, DEFAULT_LINE_SPACING)
                rowsSpacing = getDimensionPixelOffset(R.styleable.seat_reservation_rows_spacing, DEFAULT_ROWS_SPACING)
                sceneSpacing =
                    getDimensionPixelOffset(R.styleable.seat_reservation_scene_spacing, DEFAULT_SCENE_SPACING)
                rowsTextPadding =
                    getDimensionPixelOffset(R.styleable.seat_reservation_row_text_padding, DEFAULT_ROW_TEXT_PADDING)

                itemSize = getDimensionPixelOffset(R.styleable.seat_reservation_item_size, DEFAULT_ITEM_SIZE)
                _sceneWidth = getLayoutDimension(R.styleable.seat_reservation_scene_width, DEFAULT_SCENE_WIDTH)
                sceneWidth = _sceneWidth
                sceneHeight = getDimensionPixelOffset(R.styleable.seat_reservation_scene_height, DEFAULT_SCENE_HEIGHT)
                selectedTextSize =
                    getDimensionPixelOffset(R.styleable.seat_reservation_selected_text_size, DEFAULT_SELECTED_TEXT_SIZE)
                rowTextSize = getDimensionPixelOffset(R.styleable.seat_reservation_row_text_size, DEFAULT_ROW_TEXT_SIZE)

                sceneDrawable = getResourceId(R.styleable.seat_reservation_scene_drawable, DEFAULT_SCENE_DRAWABLE)
                itemDrawable = getResourceId(R.styleable.seat_reservation_item_drawable, DEFAULT_ITEM_DRAWABLE)
                selectedFontReference =
                    getResourceId(R.styleable.seat_reservation_selected_text_font, DEFAULT_SELECTED_TEXT_FONT)
                rowFontReference = getResourceId(R.styleable.seat_reservation_selected_text_font, DEFAULT_ROW_TEXT_FONT)

            } finally {
                recycle()
            }
        }
        itemBitmap = generateBitmap(context, itemDrawable)
        sceneBitmap = generateBitmap(context, sceneDrawable)

        selectedPaint.apply {
            colorFilter = PorterDuffColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        }
        freePaint.apply {
            colorFilter = PorterDuffColorFilter(freeColor, PorterDuff.Mode.SRC_IN)
        }
        bookedPaint.apply {
            colorFilter = PorterDuffColorFilter(bookedColor, PorterDuff.Mode.SRC_IN)
        }
        selectedTextPaint.apply {
            color = selectedTextColor
            textAlign = Paint.Align.CENTER
            textSize = selectedTextSize.toFloat()
            typeface = if (selectedFontReference != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resources.getFont(selectedFontReference)
            } else {
                Typeface.DEFAULT
            }
        }
        rowTextPaint.apply {
            color = rowTextColor
            textSize = rowTextSize.toFloat()
            typeface = if (rowFontReference != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resources.getFont(rowFontReference)
            } else {
                Typeface.DEFAULT
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthByItems = calculateViewWidthByItems
        val heightByItems = calculateViewHeightByItems
        var width = calculateDefaultSize(widthByItems, widthMeasureSpec)
        var height = calculateDefaultSize(heightByItems, heightMeasureSpec)

        if (recalculateItemSize(width, height, widthByItems, heightByItems)) {
            width = calculateDefaultSize(calculateViewWidthByItems, widthMeasureSpec)
            height = calculateDefaultSize(calculateViewHeightByItems, heightMeasureSpec)

            recalculateSelectedTextSize()
            recalculateRowTextSize()

            updateRowsDisplay()

        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (_sceneWidth == -1) {
            sceneWidth = width
        }

        sceneRect.apply {
            left = (width - sceneWidth) / 2
            right = left + sceneWidth
            top = 0
            bottom = sceneHeight
        }

        rowsDisplay = prepareRowsDisplay(map)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawScene()
            drawHall()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return handleClick(event)
    }

    fun updateMap(map: Array<Array<SeatReservationState>>) {
        this.map = map
        rowsDisplay = prepareRowsDisplay(map)
        requestLayout()
        invalidate()
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun deleteOnClickListener() {
        onItemClickListener = null
    }

    fun setSelectedTextFont(typeface: Typeface) {
        selectedTextPaint.typeface = typeface
        invalidate()
    }

    fun setRowTextFont(typeface: Typeface) {
        rowTextPaint.typeface = typeface
        invalidate()
    }

    fun setItemSize(newItemSize: Int) {
        itemSize = newItemSize
        requestLayout()
        invalidate()
    }

    fun setItemSpacing(newItemSpacing: Int) {
        itemSpacing = newItemSpacing
        requestLayout()
        invalidate()
    }

    fun setLineSpacing(newLineSpacing: Int) {
        lineSpacing = newLineSpacing
        requestLayout()
        invalidate()
    }

    fun setSceneSpacing(newSceneSpacing: Int) {
        sceneSpacing = newSceneSpacing
        requestLayout()
        invalidate()
    }

    fun setSceneWidth(newSceneWidth: Int) {
        sceneWidth = newSceneWidth
        requestLayout()
        invalidate()
    }

    fun setSceneHeight(newSceneHeight: Int) {
        sceneHeight = newSceneHeight
        requestLayout()
        invalidate()
    }

    fun setSelectedColor(newColor: Int) {
        selectedColor = newColor
        selectedPaint.colorFilter = PorterDuffColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    fun setFreeColor(newColor: Int) {
        freeColor = newColor
        freePaint.colorFilter = PorterDuffColorFilter(freeColor, PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    fun setBookedColor(newColor: Int) {
        bookedColor = newColor
        bookedPaint.colorFilter = PorterDuffColorFilter(bookedColor, PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    fun setSelectedTextSize(newSize: Int) {
        selectedTextSize = newSize
        selectedPaint.textSize = selectedTextSize.toFloat()
        requestLayout()
        invalidate()
    }

    fun setRowTextSize(newSize: Int) {
        rowTextSize = newSize
        rowTextPaint.textSize = rowTextSize.toFloat()
        requestLayout()
        invalidate()
    }

    fun setSelectedTextColor(newColor: Int) {
        selectedTextColor = newColor
        selectedTextPaint.color = newColor
        invalidate()
    }

    fun setRowTextColor(newColor: Int) {
        rowTextColor = newColor
        rowTextPaint.color = newColor
        invalidate()
    }

    fun setRowTextPadding(newPadding: Int) {
        rowsTextPadding = newPadding
        requestLayout()
        invalidate()
    }

    fun setRowSpacing(newSpacing: Int) {
        rowsSpacing = newSpacing
        requestLayout()
        invalidate()
    }

    fun setItemDrawable(newDrawable: Int) {
        itemDrawable = newDrawable
        itemBitmap = generateBitmap(context, itemDrawable)
        invalidate()
    }

    fun setSceneDrawable(newDrawable: Int) {
        sceneDrawable = newDrawable
        sceneBitmap = generateBitmap(context, sceneDrawable)
        invalidate()
    }

    private fun Canvas.drawScene() {
        drawBitmap(sceneBitmap, null, sceneRect, hallPaint)
    }

    private fun Canvas.drawHall() {
        rowsDisplay.forEach { rowDisplay ->
            rowDisplay?.draw(this)
        }
    }

    private fun updateRowsDisplay() {
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

    private fun prepareRowsDisplay(map: Array<Array<SeatReservationState>>): Array<RowDisplay?> {
        var rowPosition = 1
        return map.mapIndexed { rowIndex, array ->
            if (array.count { state -> state != EMPTY } > 0) {
                var placePosition = 1
                RowDisplay(rowPosition++, getYPositionByIndex(rowIndex), array.mapIndexed { placeIndex, state ->
                    if (state != EMPTY) {
                        PlaceDisplay(
                            state,
                            placePosition++,
                            getPositionByIndexes(Point(placeIndex, rowIndex)),
                            itemBitmap
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


    private fun calculateDefaultSize(calculatingSize: Int, measureSpec: Int): Int {
        val measuredSize = MeasureSpec.getSize(measureSpec)

        val size = when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.UNSPECIFIED -> measuredSize
            MeasureSpec.EXACTLY -> measuredSize
            MeasureSpec.AT_MOST -> calculatingSize
            else -> error(MEASURED_ERROR)
        }

        return size
    }

    /**
     *
     * The value of item size and item spacing is recalculated, depending on the set view sizes.
     * They will be changed if the set dimensions of the view do not correspond to the calculations by the dimensions of the view elements.
     *
     *
     * @param width measured width
     * @param height measured height
     * @param widthByItems the width obtained based on the size of items
     * @param heightByItems the height obtained based on the size of items
     *
     * @return true, if there was a recalculation of the size, false if not
     */
    private fun recalculateItemSize(width: Int, height: Int, widthByItems: Int, heightByItems: Int): Boolean {

        fun recalculateItemSize(size: Int) {
            val count = max(map.size, map.maxFrom(0) { it.size })
            val calculateItemSize =
                size / (DEFAULT_ITEM_SPACE_RATIO * count + (1 - DEFAULT_ITEM_SPACE_RATIO) * (count - 1))
            itemSize = (calculateItemSize * DEFAULT_ITEM_SPACE_RATIO).roundToInt()
            itemSpacing = (calculateItemSize * (1 - DEFAULT_ITEM_SPACE_RATIO)).roundToInt()
            lineSpacing = itemSpacing
        }

        if (heightByItems != height && widthByItems != width) {
            recalculateItemSize(min(width - (rowsTextPadding + rowsSpacing) * 2, height - sceneSpacing - sceneHeight))
            return true
        }
        if (heightByItems != height) {
            recalculateItemSize(height - sceneSpacing - sceneHeight)
            return true
        }
        if (widthByItems != width) {
            recalculateItemSize(width - (rowsTextPadding + rowsSpacing) * 2)
            return true
        }

        return false
    }

    private fun recalculateSelectedTextSize() {
        selectedTextSize = (itemSize * DEFAULT_ITEM_SELECTED_TEXT_RATIO).toInt()
        selectedTextPaint.textSize = selectedTextSize.toFloat()
    }

    private fun recalculateRowTextSize() {
        rowTextSize = (itemSize * DEFAULT_ITEM_ROW_TEXT_RATIO).toInt()
        rowTextPaint.textSize = rowTextSize.toFloat()
    }

    private fun getPaintByState(state: SeatReservationState): Paint? = when (state) {
        SELECTED -> selectedPaint
        BOOKED -> bookedPaint
        FREE -> freePaint
        EMPTY -> null
    }

    private fun getPositionByIndexes(indexes: Point): Point {
        return Point().apply {
            x = rowsSpacing + indexes.x * (itemSpacing + itemSize)
            y = getYPositionByIndex(indexes.y)
        }
    }

    private fun getYPositionByIndex(index: Int) = sceneHeight + sceneSpacing + index * (lineSpacing + itemSize)

    private fun getIndexesByPosition(position: Point): Point {
        return Point().apply {
            x = (position.x - rowsSpacing) / (itemSize + itemSpacing)
            y = (position.y - sceneHeight - sceneSpacing) / (itemSize + lineSpacing)
        }
    }

    private fun handleClick(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val position = Point(event.x.roundToInt(), event.y.roundToInt())
            val indexes = getIndexesByPosition(position)
            val rowDisplay = rowsDisplay.getOrNull(indexes.y)
            val placeDisplay = rowDisplay?.placeDisplays?.getOrNull(indexes.x)
            if (placeDisplay != null) {
                val newState = placeDisplay.click()
                map[indexes.y][indexes.x] = newState
                onItemClickListener?.onItemClick(newState, Pair(rowDisplay.getRowNumber(), placeDisplay.getSeatPosition()))
                invalidate()
            }
        }
        return false
    }

    enum class SeatReservationState(val index: Int) {
        SELECTED(1), BOOKED(2), FREE(3), EMPTY(0)
    }

    fun interface OnItemClickListener {
        /**
         * @param newState New state after clicking on the seat
         * @param seatNumber The number of the seat, the first parameter is a row, the second is a number in a row
         */
        fun onItemClick(newState: SeatReservationState, seatNumber: Pair<Int, Int>)
    }

    /**
     * A class for visualizing a row, stores the
     * seat status, number, position, bitmap for rendering, and methods for rendering,
     * obtaining a position, processing a click and updating basic parameters
     */
    private inner class PlaceDisplay(
        private var state: SeatReservationState,
        private var seatPosition: Int,
        private val positionPoint: Point,
        private val itemBitmap: Bitmap,
    ) {
        private val rect: Rect = Rect()

        init {
            updateRect()
        }

        fun draw(canvas: Canvas) {
            canvas.drawBitmap(itemBitmap, null, rect, getPaintByState(state))
            if (state == SELECTED) {
                canvas.drawText(seatPosition.toString(), rect, selectedTextPaint, Paint.Align.CENTER)
            }
        }

        fun getSeatPosition(): Int {
            return seatPosition
        }

        fun click(): SeatReservationState {
            if (state == FREE) {
                state = SELECTED
            } else if (state == SELECTED) {
                state = FREE
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

    /**
     * A class for visualizing a row
     */
    private inner class RowDisplay(
        private val rowNumber: Int,
        private var position: Int,
        val placeDisplays: Array<PlaceDisplay?>
    ) {
        private val rectLeft = Rect()
        private val rectRight = Rect()

        init {
            updateRectangles(rowsTextPadding, rowsSpacing)
        }

        fun draw(canvas: Canvas) {
            canvas.apply {
                placeDisplays.forEach { placeDisplay ->
                    drawRaw(this)
                    placeDisplay?.draw(this)
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

        private fun drawRaw(canvas: Canvas) {
            canvas.apply {
                drawText(rowNumber.toString(), rectLeft, rowTextPaint, Paint.Align.LEFT)
                drawText(rowNumber.toString(), rectRight, rowTextPaint, Paint.Align.RIGHT)
            }
        }
    }

    companion object {
        const val DEFAULT_SELECTED_COLOR = Color.GREEN
        const val DEFAULT_FREE_COLOR = Color.GRAY
        const val DEFAULT_BOOKED_COLOR = Color.RED
        const val DEFAULT_SELECTED_TEXT_COLOR = Color.BLACK
        const val DEFAULT_ROW_TEXT_COLOR = Color.BLACK

        const val DEFAULT_ITEM_SPACING = 10
        const val DEFAULT_LINE_SPACING = 10
        const val DEFAULT_ROWS_SPACING = 100
        const val DEFAULT_SCENE_SPACING = 40
        const val DEFAULT_ROW_TEXT_PADDING = 20

        const val DEFAULT_SCENE_DRAWABLE = R.drawable.default_scene_shape
        const val DEFAULT_ITEM_DRAWABLE = R.drawable.default_shape

        const val DEFAULT_ITEM_SIZE = 200
        const val DEFAULT_ROW_TEXT_SIZE = 20
        const val DEFAULT_SELECTED_TEXT_SIZE = 40
        const val DEFAULT_SCENE_WIDTH = -1
        const val DEFAULT_SCENE_HEIGHT = 100

        const val DEFAULT_SELECTED_TEXT_FONT = -1
        const val DEFAULT_ROW_TEXT_FONT = -1

        private val TEST_MAP = arrayOf(
            arrayOf(EMPTY, EMPTY, EMPTY, BOOKED, BOOKED, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, SELECTED, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, BOOKED, BOOKED, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, BOOKED, BOOKED, BOOKED, BOOKED, BOOKED, BOOKED, SELECTED, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
            arrayOf(FREE, FREE, FREE, FREE, BOOKED, BOOKED, FREE, BOOKED, BOOKED, BOOKED, FREE, FREE, FREE),
        )
    }

}