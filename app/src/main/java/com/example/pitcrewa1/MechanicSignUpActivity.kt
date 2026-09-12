package com.example.pitcrewa1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pitcrewa1.data.Mechanic
import com.example.pitcrewa1.utils.SessionManager
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.util.Locale
import java.util.UUID

class MechanicSignUpActivity : AppCompatActivity(), LocationListener {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBusinessName: EditText
    private lateinit var etSignUpEmail: EditText
    private lateinit var etSignUpPassword: EditText
    private lateinit var tvShopAddress: TextView
    private lateinit var btnGpsCurrent: TextView
    private lateinit var mapShopLocation: MapView
    private lateinit var tvTermsNotice: TextView
    private lateinit var btnSignUpSubmit: View
    private lateinit var tvSignUpSubmitText: TextView
    private lateinit var pbSignUp: ProgressBar
    private lateinit var tvBackToLogin: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var locationManager: LocationManager

    private var selectedLatitude: Double = 6.9271
    private var selectedLongitude: Double = 79.8612
    private var selectedAddress: String = "Pacific Palisades, Colombo"

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchCurrentGpsLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("pitcrew_mechanic_osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "PitcrewAutoCareMechanicApp/1.0"

        setContentView(R.layout.activity_mechanic_sign_up)

        sessionManager = SessionManager(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val rootScroll = findViewById<View>(R.id.mechanicSignUpScrollRoot)
        val headerContainer = findViewById<View>(R.id.mechanicSignUpHeaderContainer)

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

        etFirstName = findViewById(R.id.etMechanicFirstName)
        etLastName = findViewById(R.id.etMechanicLastName)
        etPhone = findViewById(R.id.etMechanicPhone)
        etBusinessName = findViewById(R.id.etMechanicBusinessName)
        etSignUpEmail = findViewById(R.id.etMechanicSignUpEmail)
        etSignUpPassword = findViewById(R.id.etMechanicSignUpPassword)
        tvShopAddress = findViewById(R.id.tvMechanicShopAddress)
        btnGpsCurrent = findViewById(R.id.btnMechanicGpsCurrent)
        mapShopLocation = findViewById(R.id.mapShopLocation)
        tvTermsNotice = findViewById(R.id.tvMechanicTermsNotice)
        btnSignUpSubmit = findViewById(R.id.btnMechanicSignUpSubmit)
        tvSignUpSubmitText = findViewById(R.id.tvMechanicSignUpSubmitText)
        pbSignUp = findViewById(R.id.pbMechanicSignUp)
        tvBackToLogin = findViewById(R.id.tvMechanicBackToLogin)

        formatTermsNotice()
        setupShopMapView()
        setupListeners()
        checkLocationPermissions()
    }

    private fun setupShopMapView() {
        val esriSource = object : OnlineTileSourceBase(
            "ESRI_WorldStreetMap",
            0,
            19,
            256,
            ".jpg",
            arrayOf(
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$baseUrl$zoom/$y/$x"
            }
        }

        mapShopLocation.setTileSource(esriSource)
        mapShopLocation.setMultiTouchControls(true)
        mapShopLocation.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapShopLocation.controller.setZoom(15.5)

        val defaultPoint = GeoPoint(selectedLatitude, selectedLongitude)
        mapShopLocation.controller.setCenter(defaultPoint)

        mapShopLocation.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapShopLocation.mapCenter
                selectedLatitude = center.latitude
                selectedLongitude = center.longitude
                reverseGeocodeShopLocation(selectedLatitude, selectedLongitude)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return true
            }
        })
    }

    private fun checkLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchCurrentGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentGpsLocation() {
        try {
            val isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var lastLoc: Location? = null
            if (isGps) {
                lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (lastLoc == null && isNetwork) {
                lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (lastLoc != null) {
                onLocationChanged(lastLoc)
            } else if (isGps) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null)
            } else if (isNetwork) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        selectedLatitude = location.latitude
        selectedLongitude = location.longitude
        val point = GeoPoint(location.latitude, location.longitude)
        mapShopLocation.controller.animateTo(point)
        reverseGeocodeShopLocation(location.latitude, location.longitude)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun reverseGeocodeShopLocation(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses: List<Address> ->
                    runOnUiThread {
                        val address = addresses.firstOrNull()
                        val text = if (address != null) {
                            val thoroughfare = address.thoroughfare ?: address.subLocality ?: address.locality ?: "Shop Location"
                            val locality = address.locality ?: address.subAdminArea ?: ""
                            "📍 $thoroughfare${if (locality.isNotEmpty()) ", $locality" else ""}"
                        } else {
                            String.format(Locale.US, "📍 %.4f, %.4f", lat, lng)
                        }
                        selectedAddress = text.removePrefix("📍 ")
                        tvShopAddress.text = text
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                val text = if (address != null) {
                    val thoroughfare = address.thoroughfare ?: address.subLocality ?: address.locality ?: "Shop Location"
                    val locality = address.locality ?: address.subAdminArea ?: ""
                    "📍 $thoroughfare${if (locality.isNotEmpty()) ", $locality" else ""}"
                } else {
                    String.format(Locale.US, "📍 %.4f, %.4f", lat, lng)
                }
                selectedAddress = text.removePrefix("📍 ")
                tvShopAddress.text = text
            }
        } catch (e: Exception) {
            val text = String.format(Locale.US, "📍 %.4f, %.4f", lat, lng)
            selectedAddress = text.removePrefix("📍 ")
            tvShopAddress.text = text
        }
    }

    private fun setupListeners() {
        btnGpsCurrent.setOnClickListener {
            checkLocationPermissions()
        }

        btnSignUpSubmit.setOnClickListener {
            validateAndRegisterMechanic()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateAndRegisterMechanic() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val businessName = etBusinessName.text.toString().trim()
        val email = etSignUpEmail.text.toString().trim()
        val password = etSignUpPassword.text.toString().trim()

        if (firstName.isEmpty()) {
            etFirstName.error = "Please enter first name"
            etFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            etLastName.error = "Please enter last name"
            etLastName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etPhone.error = "Please enter phone number"
            etPhone.requestFocus()
            return
        }

        if (businessName.isEmpty()) {
            etBusinessName.error = "Please enter workshop / business name"
            etBusinessName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etSignUpEmail.error = "Please enter email address"
            etSignUpEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignUpEmail.error = "Please enter a valid email address"
            etSignUpEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            etSignUpPassword.error = "Password must be at least 6 characters"
            etSignUpPassword.requestFocus()
            return
        }

        setLoadingState(true)

        val mechanicId = "mech_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 5)}"
        val mechanic = Mechanic(
            mechanicId = mechanicId,
            firstName = firstName,
            lastName = lastName,
            fullName = "$firstName $lastName",
            phone = phone,
            businessName = businessName,
            email = email,
            password = password,
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            address = selectedAddress,
            isOnline = true,
            isVerified = true,
            rating = 5.0,
            totalJobsCompleted = 0,
            createdAt = System.currentTimeMillis()
        )

        // Write directly to dedicated /mechanics/{mechanicId} Firebase table
        val mechanicsRef = FirebaseDatabase.getInstance().getReference("mechanics")
        mechanicsRef.child(mechanicId).setValue(mechanic)
            .addOnSuccessListener {
                setLoadingState(false)
                sessionManager.saveMechanicSession(
                    mechanicId = mechanicId,
                    firstName = firstName,
                    lastName = lastName,
                    businessName = businessName,
                    email = email,
                    phone = phone,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    address = selectedAddress
                )
                Toast.makeText(this, "Mechanic account created successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MechanicMainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                // Offline fallback support
                setLoadingState(false)
                sessionManager.saveMechanicSession(
                    mechanicId = mechanicId,
                    firstName = firstName,
                    lastName = lastName,
                    businessName = businessName,
                    email = email,
                    phone = phone,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    address = selectedAddress
                )
                Toast.makeText(this, "Account created! Sync queued.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MechanicMainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
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

    override fun onResume() {
        super.onResume()
        mapShopLocation.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapShopLocation.onPause()
    }
}
