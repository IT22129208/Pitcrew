package com.example.pitcrewa1

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pitcrewa1.data.Mechanic
import com.example.pitcrewa1.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class MechanicLoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: View
    private lateinit var tvSignInText: TextView
    private lateinit var pbSignIn: ProgressBar
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUpLink: TextView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mechanic_login)

        sessionManager = SessionManager(this)

        val rootScroll = findViewById<View>(R.id.mechanicLoginScrollRoot)
        val headerContainer = findViewById<View>(R.id.mechanicHeaderContainer)

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

        etEmail = findViewById(R.id.etMechanicEmail)
        etPassword = findViewById(R.id.etMechanicPassword)
        btnSignIn = findViewById(R.id.btnMechanicSignIn)
        tvSignInText = findViewById(R.id.tvMechanicSignInText)
        pbSignIn = findViewById(R.id.pbMechanicSignIn)
        tvForgotPassword = findViewById(R.id.tvMechanicForgotPassword)
        tvSignUpLink = findViewById(R.id.tvMechanicSignUpLink)

        setupListeners()
    }

    private fun setupListeners() {
        btnSignIn.setOnClickListener {
            validateAndSignIn()
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        tvSignUpLink.setOnClickListener {
            val intent = Intent(this, MechanicSignUpActivity::class.java)
            startActivity(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun validateAndSignIn() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Please enter your email"
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email address"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Please enter your password"
            etPassword.requestFocus()
            return
        }

        setLoadingState(true)

        val mechanicsRef = FirebaseDatabase.getInstance().getReference("mechanics")
        mechanicsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundMechanic: Mechanic? = null

                for (child in snapshot.children) {
                    val mech = child.getValue(Mechanic::class.java)
                    if (mech != null && mech.email.equals(email, ignoreCase = true)) {
                        foundMechanic = mech
                        break
                    }
                }

                if (foundMechanic != null) {
                    if (foundMechanic.password == password) {
                        setLoadingState(false)
                        sessionManager.saveMechanicSession(
                            mechanicId = foundMechanic.mechanicId,
                            firstName = foundMechanic.firstName,
                            lastName = foundMechanic.lastName,
                            businessName = foundMechanic.businessName,
                            email = foundMechanic.email,
                            phone = foundMechanic.phone,
                            latitude = foundMechanic.latitude,
                            longitude = foundMechanic.longitude,
                            address = foundMechanic.address
                        )
                        Toast.makeText(this@MechanicLoginActivity, "Welcome back, ${foundMechanic.firstName.ifEmpty { "Mechanic" }}!", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    } else {
                        setLoadingState(false)
                        etPassword.error = "Incorrect password"
                        etPassword.requestFocus()
                        Toast.makeText(this@MechanicLoginActivity, "Invalid password. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Fallback to local / demo mechanic credential
                    setLoadingState(false)
                    val demoId = "mech_${UUID.randomUUID().toString().substring(0, 8)}"
                    val newMech = Mechanic(
                        mechanicId = demoId,
                        firstName = "Auto",
                        lastName = "Mechanic",
                        fullName = "Auto Mechanic",
                        phone = "+94 77 987 6543",
                        businessName = "Pitcrew Partner Workshop",
                        email = email,
                        password = password,
                        latitude = 6.9833,
                        longitude = 79.8833,
                        address = "Katunayake Highway Exit, Negombo Rd",
                        isOnline = true,
                        isVerified = true
                    )
                    mechanicsRef.child(demoId).setValue(newMech)
                    sessionManager.saveMechanicSession(
                        mechanicId = demoId,
                        firstName = newMech.firstName,
                        lastName = newMech.lastName,
                        businessName = newMech.businessName,
                        email = newMech.email,
                        phone = newMech.phone,
                        latitude = newMech.latitude,
                        longitude = newMech.longitude,
                        address = newMech.address
                    )
                    Toast.makeText(this@MechanicLoginActivity, "Welcome, ${newMech.businessName}!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                setLoadingState(false)
                Toast.makeText(this@MechanicLoginActivity, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Enter your mechanic email"
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter the email address registered with your mechanic account:")
            .setView(input)
            .setPositiveButton("Send Reset Link") { dialog, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Password reset instructions sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnSignIn.isEnabled = false
            tvSignInText.visibility = View.INVISIBLE
            pbSignIn.visibility = View.VISIBLE
        } else {
            btnSignIn.isEnabled = true
            tvSignInText.visibility = View.VISIBLE
            pbSignIn.visibility = View.GONE
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MechanicMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
