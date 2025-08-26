package com.example.crowdshift

import android.animation.*
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.random.Random

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var logoContainer: View
    private lateinit var letterC: TextView
    private lateinit var letterR1: TextView
    private lateinit var letterO1: TextView
    private lateinit var letterW: TextView
    private lateinit var letterD: TextView
    private lateinit var letterS: TextView
    private lateinit var letterH: TextView
    private lateinit var letterI: TextView
    private lateinit var letterF: TextView
    private lateinit var letterT: TextView
    private lateinit var taglineText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var pin1: ImageView
    private lateinit var pin2: ImageView
    private lateinit var pin3: ImageView
    private lateinit var pin4: ImageView
    private lateinit var pin5: ImageView
    private lateinit var pin6: ImageView
    private lateinit var pin7: ImageView

    private val floatingAnimators = mutableListOf<ObjectAnimator>()

    private companion object {
        const val SPLASH_DELAY = 4500L // Extended for more spectacular show
        const val LETTER_ANIMATION_DURATION = 800L
        const val PIN_BOUNCE_DURATION = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alive_splash)

        initializeViews()
        startSpectacularAnimations()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMainActivity()
        }, SPLASH_DELAY)
    }

    private fun initializeViews() {
        logoContainer = findViewById(R.id.logo_container)
        letterC = findViewById(R.id.letter_c)
        letterR1 = findViewById(R.id.letter_r1)
        letterO1 = findViewById(R.id.letter_o1)
        letterW = findViewById(R.id.letter_w)
        letterD = findViewById(R.id.letter_d)
        letterS = findViewById(R.id.letter_s)
        letterH = findViewById(R.id.letter_h)
        letterI = findViewById(R.id.letter_i)
        letterF = findViewById(R.id.letter_f)
        letterT = findViewById(R.id.letter_t)

        taglineText = findViewById(R.id.tagline_text)
        progressBar = findViewById(R.id.progress_bar)

        pin1 = findViewById(R.id.pin_1)
        pin2 = findViewById(R.id.pin_2)
        pin3 = findViewById(R.id.pin_3)
        pin4 = findViewById(R.id.pin_4)
        pin5 = findViewById(R.id.pin_5)
        pin6 = findViewById(R.id.pin_6)
        pin7 = findViewById(R.id.pin_7)

        // Initially hide everything
        hideAllElements()
    }

    private fun hideAllElements() {
        taglineText.alpha = 0f
        progressBar.alpha = 0f

        val letters = listOf(letterC, letterR1, letterO1, letterW, letterD, letterS, letterH, letterI, letterF, letterT)
        val pins = listOf(pin1, pin2, pin3, pin4, pin5, pin6, pin7)

        letters.forEach { letter ->
            letter.alpha = 0f
            letter.scaleX = 0f
            letter.scaleY = 0f
            letter.rotationX = -90f
            letter.translationY = -200f
        }

        pins.forEach { pin ->
            pin.alpha = 0f
            pin.scaleX = 0f
            pin.scaleY = 0f
            pin.translationY = -300f
        }
    }

    private fun startSpectacularAnimations() {
        // Phase 1: Dramatic pin entrances (0-1000ms)
        animatePinsWithPhysics()

        // Phase 2: Letters fly in with realistic physics (500-2000ms)
        Handler(Looper.getMainLooper()).postDelayed({
            animateLettersWithRealisticPhysics()
        }, 500)

        // Phase 3: Start breathing effect for letters (2500ms)
        Handler(Looper.getMainLooper()).postDelayed({
            startBreathingEffect()
        }, 2500)

        // Phase 4: Tagline slides in elegantly (3000ms)
        Handler(Looper.getMainLooper()).postDelayed({
            animateTaglineWithTypewriter()
        }, 3000)

        // Phase 5: Progress bar with pulse effect (3500ms)
        Handler(Looper.getMainLooper()).postDelayed({
            animateProgressBarWithPulse()
        }, 3500)

        // Phase 6: Start continuous floating for pins (4000ms)
        Handler(Looper.getMainLooper()).postDelayed({
            startContinuousFloating()
        }, 4000)
    }

    private fun animatePinsWithPhysics() {
        val pins = listOf(pin1, pin2, pin3, pin4, pin5, pin6, pin7)

        pins.forEachIndexed { index, pin ->
            Handler(Looper.getMainLooper()).postDelayed({
                // Realistic drop with bounce
                val dropAnimator = ObjectAnimator.ofFloat(pin, "translationY", -300f, 20f, 0f)
                val scaleXAnimator = ObjectAnimator.ofFloat(pin, "scaleX", 0f, 1.3f, 0.9f, 1.1f, 1f)
                val scaleYAnimator = ObjectAnimator.ofFloat(pin, "scaleY", 0f, 1.3f, 0.9f, 1.1f, 1f)
                val alphaAnimator = ObjectAnimator.ofFloat(pin, "alpha", 0f, 1f)
                val rotationAnimator = ObjectAnimator.ofFloat(pin, "rotation", -180f, 0f)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(dropAnimator, scaleXAnimator, scaleYAnimator, alphaAnimator, rotationAnimator)
                animatorSet.duration = PIN_BOUNCE_DURATION
                animatorSet.interpolator = BounceInterpolator()
                animatorSet.start()

                // Add impact shake effect
                Handler(Looper.getMainLooper()).postDelayed({
                    addShakeEffect(pin)
                }, PIN_BOUNCE_DURATION - 200)

            }, index * 150L)
        }
    }

    private fun animateLettersWithRealisticPhysics() {
        val letters = listOf(letterC, letterR1, letterO1, letterW, letterD, letterS, letterH, letterI, letterF, letterT)

        letters.forEachIndexed { index, letter ->
            Handler(Looper.getMainLooper()).postDelayed({
                // Each letter flies in from different direction with physics
                val startTranslationX = when(index % 4) {
                    0 -> -400f  // From left
                    1 -> 400f   // From right
                    2 -> -200f  // From left-center
                    else -> 200f // From right-center
                }

                letter.translationX = startTranslationX

                // Multi-stage realistic animation
                val translationXAnimator = ObjectAnimator.ofFloat(letter, "translationX", startTranslationX, 0f)
                val translationYAnimator = ObjectAnimator.ofFloat(letter, "translationY", -200f, -50f, 0f)
                val rotationXAnimator = ObjectAnimator.ofFloat(letter, "rotationX", -90f, 10f, 0f)
                val scaleXAnimator = ObjectAnimator.ofFloat(letter, "scaleX", 0f, 1.2f, 1f)
                val scaleYAnimator = ObjectAnimator.ofFloat(letter, "scaleY", 0f, 1.2f, 1f)
                val alphaAnimator = ObjectAnimator.ofFloat(letter, "alpha", 0f, 0.7f, 1f)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(
                    translationXAnimator, translationYAnimator, rotationXAnimator,
                    scaleXAnimator, scaleYAnimator, alphaAnimator
                )
                animatorSet.duration = LETTER_ANIMATION_DURATION
                animatorSet.interpolator = OvershootInterpolator(1.5f)
                animatorSet.start()

                // Add secondary bounce effect
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        addLetterBounce(letter)
                    }
                })

            }, index * 100L)
        }
    }

    private fun addShakeEffect(view: View) {
        val shakeAnimator = ObjectAnimator.ofFloat(view, "translationX", 0f, -8f, 8f, -6f, 6f, -4f, 4f, 0f)
        shakeAnimator.duration = 400
        shakeAnimator.start()
    }

    private fun addLetterBounce(letter: TextView) {
        val bounceAnimator = ObjectAnimator.ofFloat(letter, "scaleY", 1f, 0.9f, 1.1f, 1f)
        bounceAnimator.duration = 300
        bounceAnimator.interpolator = BounceInterpolator()
        bounceAnimator.start()
    }

    private fun startBreathingEffect() {
        val letters = listOf(letterC, letterR1, letterO1, letterW, letterD, letterS, letterH, letterI, letterF, letterT)

        letters.forEachIndexed { index, letter ->
            val breatheAnimator = ObjectAnimator.ofFloat(letter, "scaleY", 1f, 1.05f, 1f)
            breatheAnimator.duration = 2000
            breatheAnimator.repeatCount = ObjectAnimator.INFINITE
            breatheAnimator.interpolator = AccelerateDecelerateInterpolator()
            breatheAnimator.startDelay = index * 200L
            breatheAnimator.start()

            floatingAnimators.add(breatheAnimator)

            // Add subtle rotation breathing
            val rotateBreathAnimator = ObjectAnimator.ofFloat(letter, "rotation", 0f, 1f, 0f, -1f, 0f)
            rotateBreathAnimator.duration = 3000
            rotateBreathAnimator.repeatCount = ObjectAnimator.INFINITE
            rotateBreathAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotateBreathAnimator.startDelay = index * 250L
            rotateBreathAnimator.start()

            floatingAnimators.add(rotateBreathAnimator)
        }
    }

    private fun animateTaglineWithTypewriter() {
        val fullText = "Less Congestions, More Connections"
        taglineText.text = ""

        // Fade in container first
        val alphaAnimator = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f)
        alphaAnimator.duration = 300
        alphaAnimator.start()

        // Typewriter effect
        var currentText = ""
        fullText.forEachIndexed { index, char ->
            Handler(Looper.getMainLooper()).postDelayed({
                currentText += char
                taglineText.text = currentText

                // Add subtle scale pulse for each character
                val pulseAnimator = ObjectAnimator.ofFloat(taglineText, "scaleX", 1f, 1.02f, 1f)
                pulseAnimator.duration = 100
                pulseAnimator.start()

            }, index * 50L)
        }
    }

    private fun animateProgressBarWithPulse() {
        // Fade in
        val alphaAnimator = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f)
        alphaAnimator.duration = 500
        alphaAnimator.start()

        // Continuous pulse effect
        val pulseAnimator = ObjectAnimator.ofFloat(progressBar, "scaleX", 1f, 1.1f, 1f)
        val pulseAnimatorY = ObjectAnimator.ofFloat(progressBar, "scaleY", 1f, 1.1f, 1f)

        pulseAnimator.duration = 1500
        pulseAnimatorY.duration = 1500
        pulseAnimator.repeatCount = ObjectAnimator.INFINITE
        pulseAnimatorY.repeatCount = ObjectAnimator.INFINITE
        pulseAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulseAnimatorY.interpolator = AccelerateDecelerateInterpolator()

        pulseAnimator.start()
        pulseAnimatorY.start()

        floatingAnimators.add(pulseAnimator)
        floatingAnimators.add(pulseAnimatorY)
    }

    private fun startContinuousFloating() {
        val pins = listOf(pin1, pin2, pin3, pin4, pin5, pin6, pin7)

        pins.forEachIndexed { index, pin ->
            // Random floating patterns for each pin
            val floatRange = Random.nextFloat() * 20 + 10 // 10-30px range
            val duration = Random.nextLong(2000, 4000) // 2-4 seconds

            val floatAnimator = ObjectAnimator.ofFloat(pin, "translationY", 0f, -floatRange, 0f)
            floatAnimator.duration = duration
            floatAnimator.repeatCount = ObjectAnimator.INFINITE
            floatAnimator.interpolator = AccelerateDecelerateInterpolator()
            floatAnimator.startDelay = index * 300L
            floatAnimator.start()

            floatingAnimators.add(floatAnimator)

            // Add subtle rotation
            val rotateAnimator = ObjectAnimator.ofFloat(pin, "rotation", 0f, 5f, 0f, -5f, 0f)
            rotateAnimator.duration = duration + 500
            rotateAnimator.repeatCount = ObjectAnimator.INFINITE
            rotateAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimator.startDelay = index * 400L
            rotateAnimator.start()

            floatingAnimators.add(rotateAnimator)
        }
    }

    private fun navigateToMainActivity() {
        // Stop all floating animations
        floatingAnimators.forEach { it.cancel() }

        // Epic exit animation
        val allViews = listOf(
            letterC, letterR1, letterO1, letterW, letterD, letterS, letterH, letterI, letterF, letterT,
            pin1, pin2, pin3, pin4, pin5, pin6, pin7, taglineText, progressBar
        )

        allViews.forEachIndexed { index, view ->
            val exitAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            val scaleAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f)
            val scaleAnimatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(exitAnimator, scaleAnimator, scaleAnimatorY)
            animatorSet.duration = 400
            animatorSet.startDelay = index * 50L
            animatorSet.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 600)
    }

    override fun onBackPressed() {
        // Disabled during spectacular show
    }
}