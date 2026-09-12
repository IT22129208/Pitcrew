package com.example.pitcrewa1

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pitcrewa1.utils.SessionManager

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_selection)

        // Ensure dark status bar and nav bar icons on light white background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val root = findViewById<View>(R.id.roleSelectionRoot)
        val ivLogo = findViewById<ImageView>(R.id.ivRoleLogo)
        val buttonContainer = findViewById<LinearLayout>(R.id.buttonContainer)
        val btnIamUser = findViewById<FrameLayout>(R.id.btnIamUser)
        val btnIamMechanic = findViewById<FrameLayout>(R.id.btnIamMechanic)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        // Initial animation states
        ivLogo.alpha = 0f
        ivLogo.translationY = -30f
        btnIamUser.alpha = 0f
        btnIamUser.translationY = 40f
        btnIamMechanic.alpha = 0f
        btnIamMechanic.translationY = 50f

        // Run smooth entrance animations
        playEntranceAnimations(ivLogo, btnIamUser, btnIamMechanic)

        btnIamUser.setOnClickListener {
            animateButtonPress(btnIamUser) {
                selectRoleAndNavigate("USER")
            }
        }

        btnIamMechanic.setOnClickListener {
            animateButtonPress(btnIamMechanic) {
                selectRoleAndNavigate("MECHANIC")
            }
        }
    }

    private fun playEntranceAnimations(
        logo: ImageView,
        btnUser: FrameLayout,
        btnMechanic: FrameLayout
    ) {
        val logoFade = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val logoSlide = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, -30f, 0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val userFade = ObjectAnimator.ofFloat(btnUser, View.ALPHA, 0f, 1f).apply {
            duration = 550
            startDelay = 200
            interpolator = DecelerateInterpolator()
        }
        val userSlide = ObjectAnimator.ofFloat(btnUser, View.TRANSLATION_Y, 40f, 0f).apply {
            duration = 550
            startDelay = 200
            interpolator = OvershootInterpolator(0.8f)
        }

        val mechFade = ObjectAnimator.ofFloat(btnMechanic, View.ALPHA, 0f, 1f).apply {
            duration = 550
            startDelay = 320
            interpolator = DecelerateInterpolator()
        }
        val mechSlide = ObjectAnimator.ofFloat(btnMechanic, View.TRANSLATION_Y, 50f, 0f).apply {
            duration = 550
            startDelay = 320
            interpolator = OvershootInterpolator(0.8f)
        }

        AnimatorSet().apply {
            playTogether(logoFade, logoSlide, userFade, userSlide, mechFade, mechSlide)
            start()
        }
    }

    private fun animateButtonPress(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .withEndAction {
                        onEnd()
                    }
                    .start()
            }
            .start()
    }

    private fun selectRoleAndNavigate(role: String) {
        sessionManager.saveUserRole(role)

        val intent = if (role == "MECHANIC") {
            Intent(this, MechanicLoginActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
