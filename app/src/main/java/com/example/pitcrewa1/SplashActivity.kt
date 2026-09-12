package com.example.pitcrewa1

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDurationMs: Long = 2400L
    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false
    private var progressAnimator: ValueAnimator? = null
    private var entranceAnimatorSet: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val splashRoot = findViewById<View>(R.id.splashRoot)
        val brandContainer = findViewById<LinearLayout>(R.id.brandContainer)
        val ivPitcrewLogo = findViewById<ImageView>(R.id.ivPitcrewLogo)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val splashProgressBar = findViewById<ProgressBar>(R.id.splashProgressBar)
        val bottomContainer = findViewById<LinearLayout>(R.id.bottomContainer)

        // Handle edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(splashRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Initial View States for Fluid Animation
        ivPitcrewLogo.alpha = 0f
        ivPitcrewLogo.scaleX = 0.86f
        ivPitcrewLogo.scaleY = 0.86f

        tvTagline.alpha = 0f
        tvTagline.translationY = 25f

        bottomContainer.alpha = 0f
        bottomContainer.translationY = 20f

        splashProgressBar.alpha = 0f
        splashProgressBar.progress = 0

        // Start Choreographed Brand Animations
        startSplashAnimations(
            ivPitcrewLogo,
            tvTagline,
            splashProgressBar,
            bottomContainer
        )

        // Allow immediate skip on tap
        splashRoot.setOnClickListener {
            navigateToNextScreen()
        }

        // Schedule auto-transition to Customer Dashboard
        handler.postDelayed({
            navigateToNextScreen()
        }, splashDurationMs)
    }

    private fun startSplashAnimations(
        logo: ImageView,
        tagline: TextView,
        progressBar: ProgressBar,
        bottomView: LinearLayout
    ) {
        val logoFade = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f).apply {
            duration = 850
            interpolator = DecelerateInterpolator()
        }
        val logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.86f, 1.0f).apply {
            duration = 950
            interpolator = DecelerateInterpolator(1.5f)
        }
        val logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.86f, 1.0f).apply {
            duration = 950
            interpolator = DecelerateInterpolator(1.5f)
        }

        val taglineFade = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f).apply {
            duration = 700
            startDelay = 350
            interpolator = DecelerateInterpolator()
        }
        val taglineSlide = ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, 25f, 0f).apply {
            duration = 700
            startDelay = 350
            interpolator = DecelerateInterpolator()
        }

        val progressFade = ObjectAnimator.ofFloat(progressBar, View.ALPHA, 0f, 1f).apply {
            duration = 500
            startDelay = 450
        }

        val bottomFade = ObjectAnimator.ofFloat(bottomView, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 550
        }
        val bottomSlide = ObjectAnimator.ofFloat(bottomView, View.TRANSLATION_Y, 20f, 0f).apply {
            duration = 600
            startDelay = 550
            interpolator = DecelerateInterpolator()
        }

        entranceAnimatorSet = AnimatorSet().apply {
            playTogether(
                logoFade,
                logoScaleX,
                logoScaleY,
                taglineFade,
                taglineSlide,
                progressFade,
                bottomFade,
                bottomSlide
            )
            start()
        }

        // Animate smooth progress fill
        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 1800
            startDelay = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }

    private fun navigateToNextScreen() {
        if (hasNavigated) return
        hasNavigated = true

        handler.removeCallbacksAndMessages(null)
        progressAnimator?.cancel()
        entranceAnimatorSet?.cancel()

        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        progressAnimator?.cancel()
        entranceAnimatorSet?.cancel()
    }
}
