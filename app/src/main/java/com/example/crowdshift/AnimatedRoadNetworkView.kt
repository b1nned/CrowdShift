package com.example.crowdshift

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.*

class AnimatedRoadNetworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 12f
        style = Paint.Style.STROKE
        alpha = 200
        strokeCap = Paint.Cap.ROUND
    }

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#e0e0e0")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        alpha = 230
        strokeCap = Paint.Cap.ROUND
    }

    private val junctionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 220
    }

    private var animationProgress = 0f
    private var pulsePhase = 0f
    private val roadPaths = mutableListOf<Path>()
    private val dashPaths = mutableListOf<Path>()
    private val junctionPoints = mutableListOf<PointF>()

    init {
        setupRoadNetwork()
        startAnimation()
    }

    private fun setupRoadNetwork() {
        // Clear previous paths
        roadPaths.clear()
        dashPaths.clear()
        junctionPoints.clear()

        // We'll calculate these based on actual view dimensions in onSizeChanged
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupRoadNetworkPaths(w.toFloat(), h.toFloat())
    }

    private fun setupRoadNetworkPaths(width: Float, height: Float) {
        roadPaths.clear()
        dashPaths.clear()
        junctionPoints.clear()

        val centerX = width / 2f
        val topY = height * 0.3f
        val bottomY = height * 0.5f

        // Top row connection points (for 3 pins)
        val topLeft = PointF(centerX - 120f, topY)
        val topCenter = PointF(centerX, topY)
        val topRight = PointF(centerX + 120f, topY)

        // Bottom row connection points (for 4 pins)
        val bottomLeft = PointF(centerX - 140f, bottomY)
        val bottomCenterLeft = PointF(centerX - 50f, bottomY)
        val bottomCenterRight = PointF(centerX + 50f, bottomY)
        val bottomRight = PointF(centerX + 140f, bottomY)

        // Main horizontal curves
        roadPaths.add(createCurvedPath(topLeft, topCenter, topRight))
        roadPaths.add(createCurvedPath(bottomLeft, bottomCenterLeft, bottomCenterRight, bottomRight))

        // Vertical connections
        roadPaths.add(createCurvedPath(topLeft, bottomLeft))
        roadPaths.add(createCurvedPath(topCenter, bottomCenterLeft))
        roadPaths.add(createCurvedPath(topRight, bottomRight))

        // Cross connections for network effect
        roadPaths.add(createCurvedPath(topLeft, bottomCenterRight))
        roadPaths.add(createCurvedPath(topRight, bottomCenterLeft))

        // Junction points
        junctionPoints.addAll(listOf(
            topLeft, topCenter, topRight,
            bottomLeft, bottomCenterLeft, bottomCenterRight, bottomRight
        ))
    }

    private fun createCurvedPath(vararg points: PointF): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points[0].x, points[0].y)

        when (points.size) {
            2 -> {
                // Simple curve between two points
                val midX = (points[0].x + points[1].x) / 2f
                val midY = (points[0].y + points[1].y) / 2f
                val controlY = midY + (points[1].x - points[0].x) * 0.1f // Slight curve
                path.quadTo(midX, controlY, points[1].x, points[1].y)
            }
            3 -> {
                // Smooth curve through 3 points
                path.quadTo(points[1].x, points[1].y - 20f, points[1].x, points[1].y)
                path.quadTo(points[1].x, points[1].y - 20f, points[2].x, points[2].y)
            }
            4 -> {
                // Smooth curve through 4 points
                path.quadTo(points[1].x, points[1].y - 15f, points[1].x, points[1].y)
                path.quadTo(points[2].x, points[2].y - 15f, points[2].x, points[2].y)
                path.quadTo(points[2].x, points[2].y - 15f, points[3].x, points[3].y)
            }
        }

        return path
    }

    private fun startAnimation() {
        // Main growth animation
        val growthAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float
                invalidate()
            }
        }

        // Continuous pulse animation
        val pulseAnimator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                pulsePhase = animator.animatedValue as Float
                invalidate()
            }
        }

        growthAnimator.start()
        pulseAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (roadPaths.isEmpty()) return

        // Calculate pulse effect
        val pulseIntensity = (sin(pulsePhase) * 0.3f + 0.7f)
        roadPaint.alpha = (200 * pulseIntensity).toInt()

        // Draw roads with growth effect
        roadPaths.forEachIndexed { index, path ->
            val pathMeasure = PathMeasure(path, false)
            val pathLength = pathMeasure.length
            val visibleLength = pathLength * animationProgress

            if (visibleLength > 0) {
                val visiblePath = Path()
                pathMeasure.getSegment(0f, visibleLength, visiblePath, true)
                canvas.drawPath(visiblePath, roadPaint)
            }
        }

        // Draw junction points with pulse effect
        junctionPaint.alpha = (220 * pulseIntensity).toInt()
        junctionPoints.forEach { point ->
            val radius = 6f + sin(pulsePhase) * 2f
            canvas.drawCircle(point.x, point.y, radius * animationProgress, junctionPaint)
        }
    }

    fun startNetworkAnimation() {
        visibility = VISIBLE
        startAnimation()
    }

    fun stopNetworkAnimation() {
        visibility = GONE
    }
}