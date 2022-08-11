package com.example.seatreservations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.withClip

inline fun <T, R: Comparable<R>> Array<out T>.maxFrom(default: R, choice: (T) -> R?): R {
    var max: R? = null
    this.forEach { element ->
        val component = choice(element)
        if (component != null) {
            max = if (max != null && component > max!!) {
                component
            } else {
                component
            }
        }
    }
    return max?: default
}

fun generateBitmap(context: Context, drawable: Int): Bitmap {
    val drawable = context.resources.getDrawable(drawable, context.theme)
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun Canvas.drawText(text: String, rect: Rect, paint: Paint, align: Paint.Align) {
    withClip(rect) {
        val y: Float = paint.getTextBaselineByCenter((rect.top + (rect.bottom - rect.top)/2).toFloat())
        paint.textAlign = align
        val x = when (align) {
            Paint.Align.CENTER -> {
                rect.left + (rect.right - rect.left)/2
            }
            Paint.Align.LEFT -> {
                rect.left
            }
            Paint.Align.RIGHT -> {
                rect.right
            }
        }
        drawText(text, x.toFloat(), y, paint)
    }
}

fun Paint.getTextBaselineByCenter(center: Float) = center - (descent() + ascent()) / 2