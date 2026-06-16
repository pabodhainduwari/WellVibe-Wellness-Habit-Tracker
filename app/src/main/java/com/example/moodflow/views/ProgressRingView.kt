package com.example.moodflow.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.moodflow.R

class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f
    private var targetProgress = 0f
    private var strokeWidth = 24f
    private var ringColor = Color.BLUE
    private var ringBackgroundColor = Color.LTGRAY
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    
    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = ringColor
        
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.color = ringBackgroundColor
        
        // Load colors from resources
        ringColor = ContextCompat.getColor(context, R.color.purple)
        ringBackgroundColor = ContextCompat.getColor(context, R.color.light_gray)
        
        // Set up gradients for the ring
        val sweepGradient = SweepGradient(
            width / 2f,
            height / 2f,
            intArrayOf(
                ContextCompat.getColor(context, R.color.purple),
                ContextCompat.getColor(context, R.color.mint_green),
                ContextCompat.getColor(context, R.color.purple)
            ),
            floatArrayOf(0f, 0.5f, 1f)
        )
        paint.shader = sweepGradient
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(
            strokeWidth / 2,
            strokeWidth / 2,
            w - strokeWidth / 2,
            h - strokeWidth / 2
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background circle
        canvas.drawArc(bounds, 0f, 360f, false, backgroundPaint)
        
        // Draw progress arc
        canvas.drawArc(bounds, -90f, progress * 360f, false, paint)
    }
    
    fun setProgress(value: Float, animate: Boolean = true) {
        targetProgress = value.coerceIn(0f, 1f)
        
        if (animate) {
            val animator = ValueAnimator.ofFloat(progress, targetProgress)
            animator.duration = 1000
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            animator.start()
        } else {
            progress = targetProgress
            invalidate()
        }
    }

    fun getProgress(): Float = progress
}