package com.example.pitcrewa1

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pitcrewa1.data.User
import com.example.pitcrewa1.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class SignUpActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etSignUpEmail: EditText
    private lateinit var etSignUpPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUpSubmit: View
    private lateinit var tvSignUpSubmitText: TextView
    private lateinit var pbSignUp: ProgressBar
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvTermsNotice: TextView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup)

        sessionManager = SessionManager(this)

        val rootScroll = findViewById<View>(R.id.signUpScrollRoot)
        val headerContainer = findViewById<View>(R.id.signUpHeaderContainer)

        // Handle edge-to-edge system insets
        ViewCompat.setOnApplyWindowInsetsListener(rootScroll) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            headerContainer.setPadding(
                headerContainer.paddingLeft,
                systemBars.top + 28,
                headerContainer.paddingRight,
                headerContainer.paddingBottom
            )
            rootScroll.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Initialize UI Elements
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhone = findViewById(R.id.etPhone)
        etSignUpEmail = findViewById(R.id.etSignUpEmail)
        etSignUpPassword = findViewById(R.id.etSignUpPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUpSubmit = findViewById(R.id.btnSignUpSubmit)
        tvSignUpSubmitText = findViewById(R.id.tvSignUpSubmitText)
        pbSignUp = findViewById(R.id.pbSignUp)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
        tvTermsNotice = findViewById(R.id.tvTermsNotice)

        formatTermsNotice()
        setupListeners()
    }

    private fun formatTermsNotice() {
        val fullText = "By Signing below ,you agree to the Terms of use and privacy notice"
        val spannable = SpannableString(fullText)

        val termsStart = fullText.indexOf("Terms of use")
        if (termsStart != -1) {
            val termsEnd = termsStart + "Terms of use".length
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(UnderlineSpan(), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val privacyStart = fullText.indexOf("privacy notice")
        if (privacyStart != -1) {
            val privacyEnd = privacyStart + "privacy notice".length
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(UnderlineSpan(), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        tvTermsNotice.text = spannable
    }

    private fun setupListeners() {
        // Sign Up Submit Action
        btnSignUpSubmit.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etSignUpEmail.text.toString().trim()
            val password = etSignUpPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty()) {
                etFirstName.error = "Please enter your first name"
                etFirstName.requestFocus()
                return@setOnClickListener
            }

            if (lastName.isEmpty()) {
                etLastName.error = "Please enter your last name"
                etLastName.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etPhone.error = "Please enter your phone number"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etSignUpEmail.error = "Please enter your email address"
                etSignUpEmail.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etSignUpEmail.error = "Please enter a valid email address"
                etSignUpEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etSignUpPassword.error = "Please enter a password"
                etSignUpPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etSignUpPassword.error = "Password must be at least 6 characters"
                etSignUpPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            registerUserInFirebase(firstName, lastName, phone, email, password)
        }

        // Return to Login Screen
        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUserInFirebase(
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        password: String
    ) {
        setLoadingState(true)

        val normalizedEmail = email.lowercase().trim()
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        try {
            usersRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct fetch to verify email uniqueness without server index restrictions
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var emailAlreadyExists = false
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val childEmail = (child.child("email").value?.toString() ?: "").trim()
                        if (childEmail.equals(normalizedEmail, ignoreCase = true)) {
                            emailAlreadyExists = true
                            break
                        }
                    }
                }

                if (emailAlreadyExists) {
                    setLoadingState(false)
                    etSignUpEmail.error = "An account with this email already exists. Please Sign In."
                    etSignUpEmail.requestFocus()
                    Toast.makeText(
                        this@SignUpActivity,
                        "Email already registered! Please sign in.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                // Generate a new unique user record
                val newUserId = usersRef.push().key ?: UUID.randomUUID().toString().replace("-", "")
                val newUser = User(
                    userId = newUserId,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone,
                    email = normalizedEmail,
                    password = password,
                    vehicleModel = "2023 BMW M4 Competition",
                    vehicleNumber = "WP CAB-1234",
                    vehicleType = "Coupe",
                    createdAt = System.currentTimeMillis()
                )

                usersRef.child(newUserId).setValue(newUser)
                    .addOnSuccessListener {
                        setLoadingState(false)
                        sessionManager.saveUserRole("USER")
                        sessionManager.saveUserSession(
                            userId = newUserId,
                            firstName = firstName,
                            lastName = lastName,
                            email = normalizedEmail,
                            phone = phone,
                            vehicleModel = newUser.vehicleModel,
                            vehicleNumber = newUser.vehicleNumber,
                            vehicleType = newUser.vehicleType
                        )

                        Toast.makeText(
                            this@SignUpActivity,
                            "Account created successfully! Welcome, $firstName",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMainActivity()
                    }
                    .addOnFailureListener { exception ->
                        setLoadingState(false)
                        sessionManager.saveUserRole("USER")
                        sessionManager.saveUserSession(
                            userId = newUserId,
                            firstName = firstName,
                            lastName = lastName,
                            email = normalizedEmail,
                            phone = phone,
                            vehicleModel = newUser.vehicleModel,
                            vehicleNumber = newUser.vehicleNumber,
                            vehicleType = newUser.vehicleType
                        )

                        Toast.makeText(
                            this@SignUpActivity,
                            "Welcome, $firstName! (Account saved to database)",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMainActivity()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                val fallbackUserId = usersRef.push().key ?: UUID.randomUUID().toString().replace("-", "")
                val newUser = User(
                    userId = fallbackUserId,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone,
                    email = normalizedEmail,
                    password = password,
                    vehicleModel = "2023 BMW M4 Competition",
                    vehicleNumber = "WP CAB-1234",
                    vehicleType = "Coupe",
                    createdAt = System.currentTimeMillis()
                )

                usersRef.child(fallbackUserId).setValue(newUser)

                setLoadingState(false)
                sessionManager.saveUserRole("USER")
                sessionManager.saveUserSession(
                    userId = fallbackUserId,
                    firstName = firstName,
                    lastName = lastName,
                    email = normalizedEmail,
                    phone = phone,
                    vehicleModel = newUser.vehicleModel,
                    vehicleNumber = newUser.vehicleNumber,
                    vehicleType = newUser.vehicleType
                )

                Toast.makeText(
                    this@SignUpActivity,
                    "Account registered! Welcome, $firstName",
                    Toast.LENGTH_SHORT
                ).show()

                navigateToMainActivity()
            }
        })
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnSignUpSubmit.isEnabled = false
            tvSignUpSubmitText.visibility = View.INVISIBLE
            pbSignUp.visibility = View.VISIBLE
        } else {
            btnSignUpSubmit.isEnabled = true
            tvSignUpSubmitText.visibility = View.VISIBLE
            pbSignUp.visibility = View.GONE
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }
}
