package com.example.crowdshift.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.crowdshift.R
import kotlin.math.*

class CrowdShiftLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pin data class
    data class Pin(
        var x: Float = 0f,
        var y: Float = 0f,
        var scale: Float = 1f,
        var rotation: Float = 0f,
        var color: Int = Color.RED,
        var icon: String = "📍",
        var isAnimating: Boolean = false,
        val baseScale: Float = 1f,
        val hoverScale: Float = 1.3f,
        var floatOffset: Float = 0f
    )

    // Pin configurations
    private val pins = mutableListOf<Pin>()
    private val pinColors = listOf(
        Color.parseColor("#FF6B6B"), // Red - Hotels
        Color.parseColor("#4ECDC4"), // Teal - Transport
        Color.parseColor("#45B7D1"), // Blue - Parking
        Color.parseColor("#F9CA24"), // Yellow - Restaurants
        Color.parseColor("#A29BFE")  // Purple - Security
    )

    private val pinIcons = listOf("🏨", "🚌", "🅿️", "🍴", "🛡️")
    private val pinLabels = listOf(
        "Hotels", "Transport", "Parking", "Dining", "Security"
    )

    // Paint objects
    private val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dimensions
    private var pinRadius = 60f
    private var pinHeight = 80f
    private var centerX = 0f
    private var centerY = 0f

    // Animation properties
    private var floatAnimator: ValueAnimator? = null
    private var shimmerOffset = 0f
    private var pulseScale = 1f

    // Touch handling
    private var selectedPin: Pin? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        initializePaints()
        initializePins()
        startFloatingAnimation()
        startShimmerAnimation()
        startPulseAnimation()
    }

    private fun initializePaints() {
        // Pin paint setup
        pinPaint.apply {
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
        }

        // Text paint setup
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#2D5A27")
        }

        // Shadow paint setup
        shadowPaint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#20000000")
        }

        // Icon paint setup
        iconPaint.apply {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null) // Enable shadow layer
    }

    private fun initializePins() {
        pins.clear()

        repeat(5) { index ->
            pins.add(
                Pin(
                    color = pinColors[index],
                    icon = pinIcons[index],
                    floatOffset = index * 0.5f // Stagger floating animation
                )
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = w / 2f
        centerY = h / 2f

        // Scale elements based on view size
        val scale = minOf(w, h) / 800f
        pinRadius = 60f * scale
        pinHeight = 80f * scale

        // Position pins in an arc
        positionPins()

        // Update text sizes
        textPaint.textSize = 64f * scale
        iconPaint.textSize = 32f * scale
    }

    private fun positionPins() {
        val spacing = width * 0.15f
        val startX = centerX - (pins.size - 1) * spacing / 2f
        val baseY = centerY - 100f

        pins.forEachIndexed { index, pin ->
            pin.x = startX + index * spacing
            pin.y = baseY
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background gradient (optional)
        drawBackground(canvas)

        // Draw pins with floating animation
        drawPins(canvas)

        // Draw main logo text with shimmer effect
        drawLogoText(canvas)

        // Draw tagline
        drawTagline(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.parseColor("#A8E6CF"),
                Color.parseColor("#7FCDCD"),
                Color.parseColor("#7FB3D3"),
                Color.parseColor("#5DADE2")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        val backgroundPaint = Paint().apply {
            shader = gradient
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    private fun drawPins(canvas: Canvas) {
        pins.forEach { pin ->
            canvas.save()

            // Apply transformations
            val floatY = pin.y + sin(System.currentTimeMillis() / 1000f + pin.floatOffset) * 8f
            canvas.translate(pin.x, floatY)
            canvas.scale(pin.scale, pin.scale)
            canvas.rotate(pin.rotation)

            // Draw pin shadow
            drawPinShadow(canvas)

            // Draw pin body
            drawPinBody(canvas, pin)

            // Draw pin icon
            drawPinIcon(canvas, pin)

            // Draw pulse effect if selected
            if (pin.isAnimating) {
                drawPulseEffect(canvas)
            }

            canvas.restore()
        }
    }

    private fun drawPinShadow(canvas: Canvas) {
        shadowPaint.alpha = 50
        val path = createPinPath(4f) // Slightly offset for shadow
        canvas.drawPath(path, shadowPaint)
    }

    private fun drawPinBody(canvas: Canvas, pin: Pin) {
        pinPaint.color = pin.color

        // Create gradient for pin
        val gradient = RadialGradient(
            0f, -pinRadius / 3f, pinRadius,
            intArrayOf(
                ColorUtils.lighten(pin.color, 0.3f),
                pin.color,
                ColorUtils.darken(pin.color, 0.2f)
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        pinPaint.shader = gradient

        val path = createPinPath()
        canvas.drawPath(path, pinPaint)

        pinPaint.shader = null
    }

    private fun drawPinIcon(canvas: Canvas, pin: Pin) {
        iconPaint.textSize = pinRadius * 0.6f

        // Center the icon in the pin head
        val textBounds = Rect()
        iconPaint.getTextBounds(pin.icon, 0, pin.icon.length, textBounds)

        canvas.drawText(
            pin.icon,
            0f,
            -pinRadius / 3f + textBounds.height() / 2f,
            iconPaint
        )
    }

    private fun drawPulseEffect(canvas: Canvas) {
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
            alpha = (255 * (1 - pulseScale / 2f)).toInt()
        }

        canvas.drawCircle(0f, -pinRadius / 3f, pinRadius * pulseScale, pulsePaint)
    }

    private fun createPinPath(offset: Float = 0f): Path {
        val path = Path()
        val radius = pinRadius + offset

        // Pin head (circle)
        path.addCircle(0f, -radius / 3f + offset, radius, Path.Direction.CW)

        // Pin tip (triangle)
        path.moveTo(-radius / 3f, radius / 2f + offset)
        path.lineTo(0f, pinHeight + offset)
        path.lineTo(radius / 3f, radius / 2f + offset)
        path.close()

        return path
    }

    private fun drawLogoText(canvas: Canvas) {
        val logoText = "CROWDSHIFT"

        // Create shimmer effect
        val shimmerGradient = LinearGradient(
            shimmerOffset - 200f, 0f, shimmerOffset + 200f, 0f,
            intArrayOf(
                Color.parseColor("#2D5A27"),
                Color.parseColor("#4A7C59"),
                Color.parseColor("#6A9D6B"),
                Color.parseColor("#4A7C59"),
                Color.parseColor("#2D5A27")
            ),
            floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )

        textPaint.shader = shimmerGradient

        // Draw text with shadow
        textPaint.setShadowLayer(6f, 2f, 2f, Color.parseColor("#40000000"))
        canvas.drawText(
            logoText,
            centerX,
            centerY + 120f,
            textPaint
        )

        textPaint.shader = null
        textPaint.clearShadowLayer()
    }

    private fun drawTagline(canvas: Canvas) {
        val taglineText = "Less Congestions, More Connections"

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = textPaint.textSize * 0.3f
            color = Color.parseColor("#2D5A27")
            alpha = 200
        }

        canvas.drawText(
            taglineText,
            centerX,
            centerY + 170f,
            taglinePaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                selectedPin = findTouchedPin(event.x, event.y)
                selectedPin?.let { pin ->
                    animatePinPress(pin)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                selectedPin?.let { pin ->
                    animatePinRelease(pin)
                    onPinClicked(pins.indexOf(pin))
                }
                selectedPin = null
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(event.x - lastTouchX)
                val deltaY = abs(event.y - lastTouchY)

                if (deltaX > 20 || deltaY > 20) {
                    selectedPin?.let { animatePinRelease(it) }
                    selectedPin = null
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun findTouchedPin(touchX: Float, touchY: Float): Pin? {
        return pins.find { pin ->
            val distance = sqrt(
                (touchX - pin.x).pow(2) + (touchY - pin.y).pow(2)
            )
            distance <= pinRadius * pin.scale
        }
    }

    private fun animatePinPress(pin: Pin) {
        val scaleAnimator = ObjectAnimator.ofFloat(pin, "scale", pin.scale, 1.2f).apply {
            duration = 150
            interpolator = BounceInterpolator()
        }

        val rotationAnimator = ObjectAnimator.ofFloat(pin, "rotation", 0f, 15f).apply {
            duration = 150
        }

        AnimatorSet().apply {
            playTogether(scaleAnimator, rotationAnimator)
            start()
        }

        pin.isAnimating = true
        invalidate()
    }

    private fun animatePinRelease(pin: Pin) {
        val scaleAnimator = ObjectAnimator.ofFloat(pin, "scale", pin.scale, 1f).apply {
            duration = 200
            interpolator = BounceInterpolator()
        }

        val rotationAnimator = ObjectAnimator.ofFloat(pin, "rotation", pin.rotation, 0f).apply {
            duration = 200
        }

        AnimatorSet().apply {
            playTogether(scaleAnimator, rotationAnimator)
            start()
        }

        pin.isAnimating = false
        invalidate()
    }

    private fun startFloatingAnimation() {
        floatAnimator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener {
                invalidate()
            }
        }
        floatAnimator?.start()
    }

    private fun startShimmerAnimation() {
        ValueAnimator.ofFloat(-width.toFloat(), width.toFloat() * 2).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                shimmerOffset = animator.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    private fun startPulseAnimation() {
        ValueAnimator.ofFloat(1f, 1.5f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animator ->
                pulseScale = animator.animatedValue as Float
            }
        }.start()
    }

    // Public methods
    fun onPinClicked(index: Int) {
        if (index in 0 until pins.size) {
            val label = pinLabels[index]
            // You can add a listener here for pin click events
            pinClickListener?.onPinClicked(index, label)
        }
    }

    // Pin click listener interface
    interface OnPinClickListener {
        fun onPinClicked(pinIndex: Int, pinLabel: String)
    }

    private var pinClickListener: OnPinClickListener? = null

    fun setOnPinClickListener(listener: OnPinClickListener) {
        this.pinClickListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        floatAnimator?.cancel()
    }
}

// Utility functions
object ColorUtils {
    fun lighten(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = minOf(1f, hsv[2] + factor)
        return Color.HSVToColor(hsv)
    }

    fun darken(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = maxOf(0f, hsv[2] - factor)
        return Color.HSVToColor(hsv)
    }
}