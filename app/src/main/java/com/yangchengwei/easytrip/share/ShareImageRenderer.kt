package com.yangchengwei.easytrip.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ShareImageTooLongException : Exception("行程较长，请选择一天生成")
data class ShareImage(val file: File, val width: Int, val height: Int)
data class ShareDayMap(val file: File? = null, val message: String? = null)

/** The preview and both export actions consume this same PNG; no UI screenshot is involved. */
class ShareImageRenderer(private val maxPixels: Long = minOf(24_000_000L, Runtime.getRuntime().maxMemory() / 16)) {
    private val scale = 1080f / 390f
    private val ink = Color.rgb(32,52,59)
    private val muted = Color.rgb(83,103,109)
    private val primary = Color.rgb(8,111,118)
    private val line = Color.rgb(204,221,224)
    private val noteBg = Color.rgb(240,246,246)
    private data class DrawOp(val draw: (Canvas) -> Unit)

    suspend fun render(trip: ShareTrip, options: ShareOptions, maps: Map<String, ShareDayMap>, output: File, measureOnly: Boolean = false): ShareImage = withContext(Dispatchers.Default) {
        val days = trip.selected(options)
        val calendar = projectShareCalendar(trip, options)
        require(calendar.hasContent) { "还没有可以分享的行程" }
        val ops = mutableListOf<DrawOp>()
        var y = 0f
        fun checkHeight(bottom: Float = y) {
            if (bottom * scale > 30_000 || 1080L * kotlin.math.ceil(bottom * scale).toLong() > maxPixels) throw ShareImageTooLongException()
        }
        fun drawRect(x: Float, top: Float, width: Float, height: Float, color: Int, radius: Float = 0f) {
            ops += DrawOp { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                canvas.drawRoundRect(RectF(x,top,x+width,top+height),radius,radius,paint)
            }
        }
        fun drawText(value: String, x: Float, top: Float, width: Float, size: Float, color: Int = ink, bold: Boolean = false, draw: Boolean = true): Float {
            // Refuse impossible inputs before allocating enormous StaticLayout objects.
            if (value.length > 100_000) throw ShareImageTooLongException()
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size; this.color = color
                typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val layout = StaticLayout.Builder.obtain(value,0,value.length,paint,width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(size*.35f,1f).build()
            if (draw) ops += DrawOp { c -> c.save();c.translate(x,top);layout.draw(c);c.restore() }
            return layout.height.toFloat()
        }
        fun badge(number: String, x: Float, top: Float, color: Int, size: Float = 19f) {
            drawRect(x,top,size,size,color,size/2)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=Color.WHITE;textSize=if(number.length>2)8f else 10f;textAlign=Paint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD }
            ops += DrawOp { it.drawText(number,x+size/2,top+size/2-(paint.ascent()+paint.descent())/2,paint) }
        }
        val coverOpsStart = ops.size
        y = 26f
        y += drawText("EASY TRIP · 行程分享",24f,y,342f,11f,Color.rgb(217,235,237)) + 15
        y += drawText(trip.name,24f,y,342f,27f,Color.WHITE,true) + 10
        val dates = if(days.first().date==null) "日期待定" else if(days.size==1) "${days.first().date} · 第 ${days.first().index+1} 天" else "${days.first().date} — ${days.last().date}"
        y += drawText(dates,24f,y,342f,12f,Color.rgb(217,235,237)) + 19
        drawRect(24f,y,342f,1f,0x55FFFFFF);y+=14
        y += drawText("${days.size} 天    ${days.sumOf { it.stops.size }} 站安排    ${if(options.includeNotes) "含交通与备注" else "含交通 · 不含备注"}",24f,y,342f,12f,Color.WHITE)+22
        val coverHeight=y
        ops.add(coverOpsStart,DrawOp { it.drawRect(0f,0f,390f,coverHeight,Paint().apply { color=primary }) })
        val currentCoroutineContextForDrawing = currentCoroutineContext()
        y = ShareCalendarPainter(object : ShareCalendarDrawing {
            override fun text(value: String, x: Float, y: Float, width: Float, size: Float, color: Int, bold: Boolean, draw: Boolean) =
                drawText(value, x, y, width, size, color, bold, draw)
            override fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) = drawRect(x, y, width, height, color)
            override fun outline(x: Float, y: Float, width: Float, height: Float, color: Int, dashed: Boolean) {
                ops += DrawOp { c -> c.drawRect(x, y, x + width, y + height, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color; style = Paint.Style.STROKE; strokeWidth = .7f
                    if (dashed) pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f)
                }) }
            }
            override fun checkpoint(bottom: Float) { currentCoroutineContextForDrawing.ensureActive(); checkHeight(bottom) }
        }).draw(calendar, y)
        for(day in days) {
            currentCoroutineContext().ensureActive()
            y += 24
            drawRect(24f,y,35f,35f,day.color,7f)
            drawText((day.index+1).toString().padStart(2,'0'),30f,y+6,29f,16f,Color.WHITE,true)
            drawText("第 ${day.index+1} 天",70f,y,238f,16f,ink,true)
            drawText(day.date?.format(DateTimeFormatter.ofPattern("M月d日 · EEEE",Locale.CHINA)) ?: "日期待定",70f,y+23,250f,11f,muted)
            drawText("${day.stops.size} 站",330f,y+10,40f,11f,muted)
            y+=52
            if(day.stops.isEmpty()) {
                val hasContinuation = calendar.days.first { it.day.id == day.id }.let {
                    it.visits.isNotEmpty() || it.transfers.isNotEmpty()
                }
                y += drawText(if (hasContinuation) "当天无新增安排，前日延续见上方日历" else "当天尚未安排行程",70f,y,290f,12f,muted)+24
            } else {
                val map=maps[day.id]
                val mapTop=y
                drawRect(24f,y,342f,29f,Color.WHITE)
                drawText("当天路线",35f,y+7,150f,11f,day.color,true)
                drawText("${day.stops.size} 站 · 按编号游览",241f,y+7,119f,10f,muted)
                y+=29
                if(map?.file!=null) {
                    val mapY=y; val mapFile=map.file
                    ops += DrawOp { c ->
                        val bmp=BitmapFactory.decodeFile(mapFile.absolutePath) ?: error("地图图片读取失败，请重试")
                        try { c.drawBitmap(bmp,null,RectF(24f,mapY,366f,mapY+171f),Paint(Paint.FILTER_BITMAP_FLAG)) } finally { bmp.recycle() }
                    }
                    y+=171
                } else {
                    drawRect(24f,y,342f,62f,noteBg)
                    drawText(map?.message ?: "地图暂不可用，行程清单已保留",36f,y+18,318f,11f,muted)
                    y+=62
                }
                val caption=buildList {
                    if(map?.file != null && day.hasSchematicLegs) add("虚线仅表示到访顺序")
                    if(map?.file != null) add("重叠点位合并显示编号")
                    val missing=day.stops.filter { it.point==null }.map { it.number }
                    if(missing.isNotEmpty()) add("未定位：${missing.joinToString("、")}")
                }.joinToString(" · ")
                if(caption.isNotEmpty()) y+=drawText(caption,35f,y+7,320f,10f,muted)+14
                val bottom=y
                ops+=DrawOp { c -> c.drawRoundRect(RectF(24f,mapTop,366f,bottom),7f,7f,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=line;style=Paint.Style.STROKE;strokeWidth=1f }) }
                y+=22
                for(stop in day.stops) {
                    currentCoroutineContext().ensureActive()
                    val top=y
                    val timeHeight=drawText(stop.arrival?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "时间\n待定",24f,y+3,47f,11f,day.color)
                    badge(stop.number.toString(),78f,y+2,day.color)
                    y+=drawText(stop.name,106f,y,260f,15f,ink,true)+5
                    stop.stayMinutes?.let { y+=drawText("停留 ${formatStay(it)}",106f,y,260f,11f,muted)+6 }
                    fun note(value:String) {
                        val before=ops.size; val noteTop=y+4
                        val height=drawText(value,117f,noteTop+8,238f,12f)+16
                        ops.add(before,DrawOp { c -> c.drawRect(106f,noteTop,366f,noteTop+height,Paint().apply { color=noteBg });c.drawRect(106f,noteTop,108f,noteTop+height,Paint().apply { color=line }) })
                        y=noteTop+height+6
                    }
                    if(options.includeNotes) stop.note?.let(::note)
                    stop.leg?.let { leg ->
                        y+=10
                        y+=drawText(leg.label,106f,y,260f,11f,muted)+5
                        if(options.includeNotes)leg.note?.let(::note)
                        drawRect(87f,top+23,1f,(y-top-23).coerceAtLeast(0f),line)
                    }
                    y=maxOf(y,top+timeHeight+3)
                    y+=18;checkHeight()
                }
            }
            drawRect(24f,y,342f,1f,line);checkHeight()
        }
        y+=18
        y+=drawText("Easy Trip",164f,y,90f,12f,primary,true)+7
        y+=drawText("行程仅供参考 · 交通时间为估算",96f,y,250f,10f,muted)+25
        checkHeight()
        val height=kotlin.math.ceil(y*scale).toInt()
        if (measureOnly) return@withContext ShareImage(output, 1080, height)
        val bitmap=Bitmap.createBitmap(1080,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE);canvas.scale(scale,scale)
            for(op in ops){currentCoroutineContext().ensureActive();op.draw(canvas)}
            output.parentFile?.mkdirs()
            try { output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) { "图片写入失败" } } }
            catch(error:Exception){output.delete();throw error}
            currentCoroutineContext().ensureActive()
            ShareImage(output,1080,height)
        } finally { bitmap.recycle() }
    }
    private fun formatStay(minutes:Int) = when {
        minutes<60 -> "$minutes 分钟"
        minutes%60==0 -> "${minutes/60} 小时"
        else -> "${minutes/60} 小时 ${minutes%60} 分钟"
    }
}
