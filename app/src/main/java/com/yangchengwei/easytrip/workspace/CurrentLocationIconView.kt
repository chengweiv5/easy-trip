package com.yangchengwei.easytrip.workspace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import kotlin.math.ceil

/** Distinct from green search pins: blue dot, white border, halo and a persistent label. */
internal class CurrentLocationIconView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val blue = Color.rgb(23, 92, 211)
    private val labelHeight = paint.fontMetrics.run { bottom - top } + 8f * density
    val anchorY: Float get() = 20f * density / layoutParams.height

    init {
        layoutParams = ViewGroup.LayoutParams(
            ceil(maxOf(44f * density, paint.measureText("我的位置") + 16f * density)).toInt(),
            ceil(42f * density + labelHeight).toInt(),
        )
        contentDescription = "我的位置"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(layoutParams.width, layoutParams.height)

    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = 20f * density
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(42, 23, 92, 211)
        canvas.drawCircle(centerX, centerY, 20f * density, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, 11f * density, paint)
        paint.color = blue
        canvas.drawCircle(centerX, centerY, 8f * density, paint)
        val top = 42f * density
        paint.color = Color.WHITE
        canvas.drawRoundRect(0f, top, width.toFloat(), height.toFloat(), 6f * density, 6f * density, paint)
        paint.color = blue
        canvas.drawText("我的位置", centerX, top + 4f * density - paint.fontMetrics.top, paint)
    }
}
