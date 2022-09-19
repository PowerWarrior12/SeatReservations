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
import com.example.seatreservations.SeatReservationState.*
import com.example.seatreservations.shapes.CircleSeatShape
import com.example.seatreservations.shapes.RectSeatShape
import com.example.seatreservations.shapes.SeatShape
import kotlin.math.roundToInt

private const val MEASURED_ERROR = "Error with measured"
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

    private var isRect: Boolean = DEFAULT_IS_RECT

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

    private val sceneRect: Rect = Rect()

    private val shape: SeatShape

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

                isRect = getBoolean(R.styleable.seat_reservation_is_rect, DEFAULT_IS_RECT)

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

        val shapeConfig = SeatShape.SeatShapeConfig(
            itemBitmap,
            width,
            height,
            itemSize,
            rowsTextPadding,
            rowsSpacing,
            ::getPaintByState,
            lineSpacing,
            itemSpacing,
            sceneHeight + sceneSpacing,
            sceneWidth
        )

        shape = if (isRect) {
            RectSeatShape(
                shapeConfig
            )
        } else {
            CircleSeatShape(
                shapeConfig
            )
        }

        shape.map = map
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthByItems = shape.calculateWidth
        val heightByItems = shape.calculateHeight
        var width = calculateDefaultSize(widthByItems, widthMeasureSpec)
        var height = calculateDefaultSize(heightByItems, heightMeasureSpec)

        if (recalculateItemSize(width, height)) {
            width = calculateDefaultSize(shape.calculateWidth, widthMeasureSpec)
            height = calculateDefaultSize(shape.calculateHeight, heightMeasureSpec)

            recalculateSelectedTextSize(shape.getItemSize())
            recalculateRowTextSize(shape.getItemSize())

            shape.updateDisplay()
        }

        shape.updateWidth(width)
        shape.updateHeight(height)

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

        shape.prepareDisplay()
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
        shape.map = map
        shape.prepareDisplay()
        requestLayout()
        invalidate()
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        shape.setOnClickListener(listener)
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
        shape.updateItemSize(itemSize)
        requestLayout()
        invalidate()
    }

    fun setItemSpacing(newItemSpacing: Int) {
        itemSpacing = newItemSpacing
        shape.updateItemSpacing(itemSpacing)
        requestLayout()
        invalidate()
    }

    fun setLineSpacing(newLineSpacing: Int) {
        lineSpacing = newLineSpacing
        shape.updateLineSpacing(lineSpacing)
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
        shape.draw(this, rowTextPaint)
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
     * The value of shapes item size and item spacing is recalculated, depending on the set view sizes.
     * They will be changed if the set dimensions of the view do not correspond to the calculations by the dimensions of the view elements.
     *
     * @param width measured width
     * @param height measured height
     *
     * @return true, if there was a recalculation of the size, false if not
     */
    private fun recalculateItemSize(width: Int, height: Int): Boolean {
        return shape.recalculateParams(width, height)
    }

    private fun recalculateSelectedTextSize(itemSize: Int) {
        selectedTextSize = (itemSize * DEFAULT_ITEM_SELECTED_TEXT_RATIO).toInt()
        selectedTextPaint.textSize = selectedTextSize.toFloat()
    }

    private fun recalculateRowTextSize(itemSize: Int) {
        rowTextSize = (itemSize * DEFAULT_ITEM_ROW_TEXT_RATIO).toInt()
        rowTextPaint.textSize = rowTextSize.toFloat()
    }

    private fun getPaintByState(state: SeatReservationState): Paint? = when (state) {
        SELECTED -> selectedPaint
        BOOKED -> bookedPaint
        FREE -> freePaint
        EMPTY -> null
    }

    private fun handleClick(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val position = Point(event.x.roundToInt(), event.y.roundToInt())
            shape.click(position) {
                invalidate()
            }
        }
        return false
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

        const val DEFAULT_IS_RECT = true

        private val TEST_MAP = arrayOf(
            arrayOf(FREE, FREE, FREE, FREE, FREE, FREE, FREE),
            arrayOf(FREE, SELECTED, FREE, FREE, FREE),
            arrayOf(FREE, FREE, FREE, FREE, FREE, FREE, FREE),
            arrayOf(BOOKED, BOOKED, BOOKED, FREE, FREE, FREE, SELECTED),
        )
    }

}