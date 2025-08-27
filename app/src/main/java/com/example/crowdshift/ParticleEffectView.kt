package com.example.crowdshift

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class ParticleEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f
    private val maxParticles = 50

    private val colors = intArrayOf(
        Color.parseColor("#0f6635"),
        Color.parseColor("#27ae60"),
        Color.parseColor("#1175cd"),
        Color.parseColor("#7fc6c5"),
        Color.parseColor("#e74c3c"),
        Color.parseColor("#f39c12")
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float,
        val color: Int,
        var rotation: Float = 0f,
        var rotationSpeed: Float = 0f
    )

    init {
        // Initialize particles
        repeat(maxParticles) {
            createParticle()
        }
    }

    private fun createParticle() {
        if (width <= 0 || height <= 0) return

        val particle = Particle(
            x = Random.nextFloat() * width,
            y = height + Random.nextFloat() * 200,
            vx = (Random.nextFloat() - 0.5f) * 2f,
            vy = -Random.nextFloat() * 3f - 1f,
            life = Random.nextFloat() * 3f + 2f,
            maxLife = Random.nextFloat() * 3f + 2f,
            size = Random.nextFloat() * 6f + 2f,
            color = colors[Random.nextInt(colors.size)],
            rotationSpeed = (Random.nextFloat() - 0.5f) * 4f
        )
        particles.add(particle)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        time += 0.016f // ~60fps

        // Update and draw particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()

            // Update particle physics
            particle.x += particle.vx
            particle.y += particle.vy
            particle.vy += 0.05f // Gravity
            particle.life -= 0.016f
            particle.rotation += particle.rotationSpeed

            // Add floating effect
            particle.x += sin(time + particle.y * 0.01f) * 0.5f

            if (particle.life <= 0 || particle.y < -100) {
                iterator.remove()
                createParticle()
                continue
            }

            // Calculate alpha based on life
            val alpha = (particle.life / particle.maxLife * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha,
                Color.red(particle.color),
                Color.green(particle.color),
                Color.blue(particle.color)
            )

            // Draw particle with rotation
            canvas.save()
            canvas.translate(particle.x, particle.y)
            canvas.rotate(particle.rotation)

            // Draw as small circle or diamond
            if (Random.nextBoolean()) {
                canvas.drawCircle(0f, 0f, particle.size, paint)
            } else {
                val path = Path().apply {
                    moveTo(0f, -particle.size)
                    lineTo(particle.size, 0f)
                    lineTo(0f, particle.size)
                    lineTo(-particle.size, 0f)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            canvas.restore()
        }

        // Continue animation
        invalidate()
    }

    fun startParticleEffect() {
        visibility = VISIBLE
        invalidate()
    }

    fun stopParticleEffect() {
        visibility = GONE
        particles.clear()
    }
}