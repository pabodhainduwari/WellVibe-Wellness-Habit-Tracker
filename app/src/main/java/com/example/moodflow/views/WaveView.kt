package com.example.moodflow.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.moodflow.R
import kotlin.math.PI
import kotlin.math.sin

class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var fillLevel = 0.65f
    private var waveOffset = 0f
    private var waveLength = 200f
    private var waveHeight = 20f
    private var waveColor = Color.BLUE
    private var animator: ValueAnimator? = null
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dropPath = Path()

    init {
        // Set up wave paint
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.blue_water)
        
        // Set up background paint (drop shape)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 2f
        backgroundPaint.color = ContextCompat.getColor(context, R.color.blue_water)
        
        // Start wave animation
        startWaveAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        waveLength = width.toFloat()
        waveHeight = height * 0.1f
        createDropPath()
    }

    private fun createDropPath() {
        dropPath.reset()
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw water drop shape
        dropPath.moveTo(centerX, centerY - height * 0.4f)
        dropPath.cubicTo(
            centerX - width * 0.4f, centerY - height * 0.2f,
            centerX - width * 0.4f, centerY + height * 0.3f,
            centerX, centerY + height * 0.4f
        )
        dropPath.cubicTo(
            centerX + width * 0.4f, centerY + height * 0.3f,
            centerX + width * 0.4f, centerY - height * 0.2f,
            centerX, centerY - height * 0.4f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw drop outline
        canvas.drawPath(dropPath, backgroundPaint)
        
        // Create and draw wave
        path.reset()
        path.moveTo(0f, height.toFloat())
        
        val fillHeight = height - (height * fillLevel)
        var x = 0f
        while (x <= width) {
            val y = fillHeight + sin((x + waveOffset) * (2f * PI / waveLength).toDouble()).toFloat() * waveHeight
            path.lineTo(x, y)
            x += 5f
        }
        
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        
        // Clip the wave to the drop shape
        canvas.save()
        canvas.clipPath(dropPath)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun startWaveAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, waveLength).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                waveOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setFillLevel(level: Float) {
        fillLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}