package com.example.moodflow.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.moodflow.R
import kotlin.math.sin

class FlameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var flameOffset = 0f
    private val flamePath = Path()
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    init {
        // Set up flame paint with gradient
        flamePaint.style = Paint.Style.FILL
        
        // Set up glow effect paint
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        
        startFlameAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Create radial gradient for the flame
        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.flame_yellow),
            ContextCompat.getColor(context, R.color.flame_orange),
            ContextCompat.getColor(context, R.color.flame_red)
        )
        flamePaint.shader = RadialGradient(
            width / 2f,
            height * 0.4f,
            width * 0.6f,
            colors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw flame shape
        flamePath.reset()
        val centerX = width / 2f
        val centerY = height * 0.6f
        
        // Base of the flame
        flamePath.moveTo(centerX - width * 0.3f, height.toFloat())
        flamePath.cubicTo(
            centerX - width * 0.3f, centerY + height * 0.2f,
            centerX - width * 0.2f, centerY,
            centerX, centerY - height * 0.3f + sin(flameOffset) * height * 0.05f
        )
        flamePath.cubicTo(
            centerX + width * 0.2f, centerY,
            centerX + width * 0.3f, centerY + height * 0.2f,
            centerX + width * 0.3f, height.toFloat()
        )
        flamePath.close()
        
        // Draw glow effect
        canvas.drawPath(flamePath, glowPaint)
        // Draw main flame
        canvas.drawPath(flamePath, flamePaint)
    }

    private fun startFlameAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                flameOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}