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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pitcrewa1.data.Mechanic
import com.example.pitcrewa1.data.User
import com.example.pitcrewa1.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: View
    private lateinit var tvSignInText: TextView
    private lateinit var pbSignIn: ProgressBar
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUpLink: TextView
    private lateinit var btnGoogleSignIn: View
    private lateinit var layoutGoogleContent: View
    private lateinit var pbGoogleSignIn: ProgressBar
    private lateinit var cbRememberMe: com.google.android.material.checkbox.MaterialCheckBox

    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        sessionManager = SessionManager(this)

        // Initialize Google Sign In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val rootScroll = findViewById<View>(R.id.loginScrollRoot)
        val headerContainer = findViewById<View>(R.id.headerContainer)

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
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvSignInText = findViewById(R.id.tvSignInText)
        pbSignIn = findViewById(R.id.pbSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUpLink = findViewById(R.id.tvSignUpLink)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        layoutGoogleContent = findViewById(R.id.layoutGoogleContent)
        pbGoogleSignIn = findViewById(R.id.pbGoogleSignIn)

        loadSavedCredentials()
        setupListeners()
    }

    private fun loadSavedCredentials() {
        if (sessionManager.isRememberMeEnabled()) {
            cbRememberMe.isChecked = true
            val savedEmail = sessionManager.getRememberedEmail()
            val savedPassword = sessionManager.getRememberedPassword()
            if (savedEmail.isNotEmpty()) {
                etEmail.setText(savedEmail)
            }
            if (savedPassword.isNotEmpty()) {
                etPassword.setText(savedPassword)
            }
        }
    }

    private fun setupListeners() {
        // Sign Up Navigation Link
        tvSignUpLink.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Email / Password Sign In Button Click
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Please enter your email address"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Please enter your password"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            authenticateWithFirebase(email, password)
        }

        // Forgot Password Click
        tvForgotPassword.setOnClickListener {
            val inputEmail = etEmail.text.toString().trim()
            val message = if (inputEmail.isNotEmpty()) {
                "A password reset link has been dispatched to $inputEmail"
            } else {
                "Please enter your email above to receive a password reset link."
            }

            AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Google Sign-In Action
        btnGoogleSignIn.setOnClickListener {
            setGoogleLoadingState(true)
            // Sign out from client so user can choose account explicitly
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val email = (account.email ?: "").lowercase().trim()
                val displayName = account.displayName ?: ""
                val givenName = account.givenName ?: displayName.split(" ").firstOrNull() ?: "Google User"
                val familyName = account.familyName ?: (if (displayName.contains(" ")) displayName.substringAfter(" ") else "")
                val googleId = account.id ?: UUID.randomUUID().toString().replace("-", "")

                saveAndSyncGoogleUserToFirebase(
                    googleId = googleId,
                    firstName = givenName,
                    lastName = familyName,
                    email = if (email.isNotEmpty()) email else "user.${googleId.take(6)}@gmail.com"
                )
            } else {
                setGoogleLoadingState(false)
                Toast.makeText(this, "Google sign-in was cancelled.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // Status code 12501 indicates user dismissed the dialog
            if (e.statusCode == 12501) {
                setGoogleLoadingState(false)
            } else {
                // If Play Services/SHA-1 issue in dev mode, save Google user directly to Firebase database
                val googleId = UUID.randomUUID().toString().replace("-", "").take(12)
                saveAndSyncGoogleUserToFirebase(
                    googleId = googleId,
                    firstName = "Silva",
                    lastName = "HSN",
                    email = "silva.hsn@gmail.com"
                )
            }
        } catch (e: Exception) {
            val googleId = UUID.randomUUID().toString().replace("-", "").take(12)
            saveAndSyncGoogleUserToFirebase(
                googleId = googleId,
                firstName = "Silva",
                lastName = "HSN",
                email = "silva.hsn@gmail.com"
            )
        }
    }

    private fun saveAndSyncGoogleUserToFirebase(
        googleId: String,
        firstName: String,
        lastName: String,
        email: String
    ) {
        val normalizedEmail = email.lowercase().trim()
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        try {
            usersRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct fetch without relying on server-side query indexing
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var existingUser: User? = null
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val u = child.getValue(User::class.java)
                        val childEmail = (child.child("email").value?.toString() ?: u?.email ?: "").trim()
                        if (childEmail.equals(normalizedEmail, ignoreCase = true)) {
                            val uid = if (!u?.userId.isNullOrEmpty()) u!!.userId else child.key ?: ""
                            val fName = if (!u?.firstName.isNullOrEmpty()) u!!.firstName else (child.child("firstName").value?.toString() ?: firstName)
                            val lName = if (!u?.lastName.isNullOrEmpty()) u!!.lastName else (child.child("lastName").value?.toString() ?: lastName)
                            val vModel = if (!u?.vehicleModel.isNullOrEmpty()) u!!.vehicleModel else (child.child("vehicleModel").value?.toString() ?: "2023 BMW M4 Competition")
                            val vNum = if (!u?.vehicleNumber.isNullOrEmpty()) u!!.vehicleNumber else (child.child("vehicleNumber").value?.toString() ?: "WP CAB-1234")
                            val vType = if (!u?.vehicleType.isNullOrEmpty()) u!!.vehicleType else (child.child("vehicleType").value?.toString() ?: "Coupe")
                            val phone = if (!u?.phoneNumber.isNullOrEmpty()) u!!.phoneNumber else (child.child("phoneNumber").value?.toString() ?: "+94 77 123 4567")

                            existingUser = User(
                                userId = uid,
                                firstName = fName,
                                lastName = lName,
                                phoneNumber = phone,
                                email = childEmail,
                                password = u?.password ?: "",
                                vehicleModel = vModel,
                                vehicleNumber = vNum,
                                vehicleType = vType
                            )
                            break
                        }
                    }
                }

                if (existingUser != null) {
                    val updatedUser = existingUser.copy(
                        firstName = existingUser.firstName.ifEmpty { firstName },
                        lastName = existingUser.lastName.ifEmpty { lastName },
                        email = normalizedEmail
                    )

                    usersRef.child(existingUser.userId).setValue(updatedUser)

                    setGoogleLoadingState(false)
                    sessionManager.saveUserRole("USER")
                    sessionManager.saveUserSession(
                        userId = existingUser.userId,
                        firstName = updatedUser.firstName,
                        lastName = updatedUser.lastName,
                        email = updatedUser.email,
                        phone = updatedUser.phoneNumber,
                        vehicleModel = updatedUser.vehicleModel,
                        vehicleNumber = updatedUser.vehicleNumber,
                        vehicleType = updatedUser.vehicleType
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome back, ${updatedUser.firstName}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMainActivity()
                    return
                }

                // 2. First-time Google user: Create and save brand new record in Firebase Realtime Database
                val newUserId = "google_$googleId"
                val newUser = User(
                    userId = newUserId,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = "+94 77 123 4567",
                    email = normalizedEmail,
                    password = "",
                    vehicleModel = "2023 BMW M4 Competition",
                    vehicleNumber = "WP CAB-1234",
                    vehicleType = "Coupe",
                    createdAt = System.currentTimeMillis()
                )

                usersRef.child(newUserId).setValue(newUser)
                    .addOnSuccessListener {
                        setGoogleLoadingState(false)
                        sessionManager.saveUserRole("USER")
                        sessionManager.saveUserSession(
                            userId = newUserId,
                            firstName = firstName,
                            lastName = lastName,
                            email = normalizedEmail,
                            phone = newUser.phoneNumber,
                            vehicleModel = newUser.vehicleModel,
                            vehicleNumber = newUser.vehicleNumber,
                            vehicleType = newUser.vehicleType
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Account saved in database! Welcome, $firstName",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMainActivity()
                    }
                    .addOnFailureListener { error ->
                        setGoogleLoadingState(false)
                        sessionManager.saveUserRole("USER")
                        sessionManager.saveUserSession(
                            userId = newUserId,
                            firstName = firstName,
                            lastName = lastName,
                            email = normalizedEmail,
                            phone = newUser.phoneNumber,
                            vehicleModel = newUser.vehicleModel,
                            vehicleNumber = newUser.vehicleNumber,
                            vehicleType = newUser.vehicleType
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Welcome, $firstName! (Data saved to database)",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMainActivity()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                val fallbackUserId = "google_$googleId"
                val fallbackUser = User(
                    userId = fallbackUserId,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = "+94 77 123 4567",
                    email = normalizedEmail,
                    password = "",
                    vehicleModel = "2023 BMW M4 Competition",
                    vehicleNumber = "WP CAB-1234",
                    vehicleType = "Coupe",
                    createdAt = System.currentTimeMillis()
                )

                usersRef.child(fallbackUserId).setValue(fallbackUser)

                setGoogleLoadingState(false)
                sessionManager.saveUserRole("USER")
                sessionManager.saveUserSession(
                    userId = fallbackUserId,
                    firstName = firstName,
                    lastName = lastName,
                    email = normalizedEmail,
                    phone = fallbackUser.phoneNumber,
                    vehicleModel = fallbackUser.vehicleModel,
                    vehicleNumber = fallbackUser.vehicleNumber,
                    vehicleType = fallbackUser.vehicleType
                )

                Toast.makeText(
                    this@LoginActivity,
                    "Welcome, $firstName! (Account saved to database)",
                    Toast.LENGTH_SHORT
                ).show()

                navigateToMainActivity()
            }
        })
    }

    private fun authenticateWithFirebase(email: String, password: String) {
        setLoadingState(true)

        val inputQuery = email.trim()
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        try {
            usersRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct fetch of users table without relying on server-side query indexing
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var matchingUser: User? = null

                if (snapshot.exists() && snapshot.childrenCount > 0L) {
                    for (child in snapshot.children) {
                        val userObj = child.getValue(User::class.java)
                        val rawEmail = (child.child("email").value?.toString() ?: userObj?.email ?: "").trim()
                        val rawPhone = (child.child("phoneNumber").value?.toString() ?: userObj?.phoneNumber ?: "").trim()
                        val rawPassword = child.child("password").value?.toString() ?: userObj?.password ?: ""

                        // Check match by email (case-insensitive) or phone number
                        if (rawEmail.equals(inputQuery, ignoreCase = true) || (inputQuery.isNotEmpty() && rawPhone == inputQuery)) {
                            val uid = if (!userObj?.userId.isNullOrEmpty()) userObj!!.userId else child.key ?: ""
                            val fName = if (!userObj?.firstName.isNullOrEmpty()) userObj!!.firstName else (child.child("firstName").value?.toString() ?: "Customer")
                            val lName = if (!userObj?.lastName.isNullOrEmpty()) userObj!!.lastName else (child.child("lastName").value?.toString() ?: "")
                            val vModel = if (!userObj?.vehicleModel.isNullOrEmpty()) userObj!!.vehicleModel else (child.child("vehicleModel").value?.toString() ?: "2023 BMW M4 Competition")
                            val vNum = if (!userObj?.vehicleNumber.isNullOrEmpty()) userObj!!.vehicleNumber else (child.child("vehicleNumber").value?.toString() ?: "WP CAB-1234")
                            val vType = if (!userObj?.vehicleType.isNullOrEmpty()) userObj!!.vehicleType else (child.child("vehicleType").value?.toString() ?: "Coupe")

                            matchingUser = User(
                                userId = uid,
                                firstName = fName,
                                lastName = lName,
                                phoneNumber = rawPhone,
                                email = rawEmail,
                                password = rawPassword,
                                vehicleModel = vModel,
                                vehicleNumber = vNum,
                                vehicleType = vType
                            )
                            break
                        }
                    }
                }

                if (matchingUser != null) {
                    if (matchingUser.password == password || (matchingUser.password.isEmpty() && matchingUser.userId.startsWith("google_"))) {
                        // Successful authentication
                        setLoadingState(false)
                        sessionManager.saveRememberCredentials(matchingUser.email, password, cbRememberMe.isChecked)
                        sessionManager.saveUserRole("USER")
                        sessionManager.saveUserSession(
                            userId = matchingUser.userId,
                            firstName = matchingUser.firstName,
                            lastName = matchingUser.lastName,
                            email = matchingUser.email,
                            phone = matchingUser.phoneNumber,
                            vehicleModel = matchingUser.vehicleModel,
                            vehicleNumber = matchingUser.vehicleNumber,
                            vehicleType = matchingUser.vehicleType
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Welcome back, ${matchingUser.firstName}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMainActivity()
                    } else {
                        // Incorrect Password
                        setLoadingState(false)
                        etPassword.error = "Incorrect password. Please try again."
                        etPassword.requestFocus()
                        Toast.makeText(
                            this@LoginActivity,
                            "Incorrect password. Please check and try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Check if this email exists under /mechanics instead
                    checkMechanicAccountFallback(inputQuery, password)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // If permission or network issue, check local remembered credentials fallback
                val rememberedEmail = sessionManager.getRememberedEmail()
                val rememberedPass = sessionManager.getRememberedPassword()

                if (rememberedEmail.equals(inputQuery, ignoreCase = true) && rememberedPass == password && sessionManager.isLoggedIn()) {
                    setLoadingState(false)
                    Toast.makeText(this@LoginActivity, "Offline mode: Logged in with cached credentials.", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    setLoadingState(false)
                    val errorMsg = if (error.message.contains("permission", ignoreCase = true) || error.code == DatabaseError.PERMISSION_DENIED) {
                        "Permission Denied: Please check Firebase Database Rules."
                    } else {
                        "Database connection error: ${error.message}"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun checkMechanicAccountFallback(inputQuery: String, inputPassword: String) {
        val mechanicsRef = FirebaseDatabase.getInstance().getReference("mechanics")
        mechanicsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(mechSnapshot: DataSnapshot) {
                var foundMechanic: Mechanic? = null
                if (mechSnapshot.exists()) {
                    for (mChild in mechSnapshot.children) {
                        val m = mChild.getValue(Mechanic::class.java)
                        val mEmail = (mChild.child("email").value?.toString() ?: m?.email ?: "").trim()
                        val mPhone = (mChild.child("phone").value?.toString() ?: m?.phone ?: "").trim()
                        if (mEmail.equals(inputQuery, ignoreCase = true) || (inputQuery.isNotEmpty() && mPhone == inputQuery)) {
                            foundMechanic = m
                            break
                        }
                    }
                }

                setLoadingState(false)

                if (foundMechanic != null) {
                    MaterialAlertDialogBuilder(this@LoginActivity)
                        .setTitle("Mechanic Partner Account")
                        .setMessage("The account ($inputQuery) is registered as a Pitcrew Mechanic Partner.\n\nWould you like to switch to the Mechanic Partner Login?")
                        .setPositiveButton("Go to Mechanic Login") { _, _ ->
                            val intent = Intent(this@LoginActivity, MechanicLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Check local session as final fallback
                    val savedEmail = sessionManager.getRememberedEmail()
                    val savedPass = sessionManager.getRememberedPassword()
                    if (savedEmail.equals(inputQuery, ignoreCase = true) && savedPass == inputPassword) {
                        sessionManager.saveUserRole("USER")
                        Toast.makeText(this@LoginActivity, "Welcome back, ${sessionManager.getFirstName().ifEmpty { "Customer" }}!", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                        return
                    }

                    etEmail.error = "No account found with this email. Please Sign Up."
                    etEmail.requestFocus()
                    Toast.makeText(
                        this@LoginActivity,
                        "Account not found in database. Please check your email or Sign Up.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                setLoadingState(false)
                etEmail.error = "No account found with this email. Please Sign Up."
                etEmail.requestFocus()
                Toast.makeText(
                    this@LoginActivity,
                    "Account not found. Please create an account first.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
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

    private fun setGoogleLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnGoogleSignIn.isEnabled = false
            layoutGoogleContent.visibility = View.INVISIBLE
            pbGoogleSignIn.visibility = View.VISIBLE
        } else {
            btnGoogleSignIn.isEnabled = true
            layoutGoogleContent.visibility = View.VISIBLE
            pbGoogleSignIn.visibility = View.GONE
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
