package com.example.moodflow.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.moodflow.R

class WaterProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var progress = 0f
    private var max = 100f
    private val strokeWidth = 20f
    private var progressColor = ContextCompat.getColor(context, R.color.blue_water)
    private var backgroundColor = ContextCompat.getColor(context, R.color.light_gray)
    
    fun setProgress(progress: Float, max: Float) {
        this.progress = progress
        this.max = max
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2f - strokeWidth / 2
        
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        // Draw background ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = backgroundColor
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Draw progress arc
        paint.color = progressColor
        val sweepAngle = 360 * (progress / max)
        canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
        
        // Draw progress text in the center
        paint.style = Paint.Style.FILL
        paint.textSize = 48f
        paint.color = ContextCompat.getColor(context, R.color.text_primary)
        val progressText = "${(progress / max * 100).toInt()}%"
        val textWidth = paint.measureText(progressText)
        canvas.drawText(progressText, centerX - textWidth / 2, centerY + 16f, paint)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        
        setMeasuredDimension(width, height)
    }
}