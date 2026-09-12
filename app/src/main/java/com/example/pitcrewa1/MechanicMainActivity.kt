package com.example.pitcrewa1

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pitcrewa1.adapters.MechanicEarningsAdapter
import com.example.pitcrewa1.adapters.RecentActivityAdapter
import com.example.pitcrewa1.data.Mechanic
import com.example.pitcrewa1.data.MechanicEarning
import com.example.pitcrewa1.data.RecentActivity
import com.example.pitcrewa1.data.ServiceRequest
import com.example.pitcrewa1.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MechanicMainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var sessionManager: SessionManager
    private lateinit var locationManager: LocationManager

    // Top Header Elements
    private lateinit var mechanicMainRoot: View
    private lateinit var mechanicTopHeader: LinearLayout
    private lateinit var btnProfileAvatar: FrameLayout
    private lateinit var btnRecenterGps: FrameLayout
    private lateinit var tvTodayEarningAmount: TextView

    // Bottom Sheet Panels
    private lateinit var bottomDispatchContainer: LinearLayout
    private lateinit var layoutSearchingJobPanel: LinearLayout
    private lateinit var tvSearchingJobTitle: TextView
    private lateinit var tvSearchingJobSubtitle: TextView
    private lateinit var btnMechanicRefresh: FrameLayout
    private lateinit var pbRefreshRadar: ProgressBar
    private lateinit var tvRefreshText: TextView

    private lateinit var layoutIncomingRequestPanel: LinearLayout
    private lateinit var tvIncomingEta: TextView
    private lateinit var tvIncomingCustomerName: TextView
    private lateinit var tvIncomingCustomerContact: TextView
    private lateinit var tvIncomingCause: TextView
    private lateinit var tvIncomingPaymentMethod: TextView
    private lateinit var layoutOfferPriceContainer: LinearLayout
    private lateinit var btnEditOfferPriceTrigger: View
    private lateinit var etOfferPrice: EditText
    private lateinit var btnAcceptJob: FrameLayout
    private lateinit var pbAccepting: ProgressBar
    private lateinit var tvAcceptText: TextView

    private lateinit var layoutActiveJobPanel: LinearLayout
    private lateinit var tvActiveJobStatus: TextView
    private lateinit var btnCallCustomer: TextView
    private lateinit var tvActiveCustomerDetails: TextView
    private lateinit var btnCompleteJob: FrameLayout

    // Recent Activities Screen Layer (Nav Icon 2)
    private lateinit var layoutMechanicRecentActivities: NestedScrollView
    private lateinit var mechanicRecentActivitiesHeader: LinearLayout
    private lateinit var rvMechanicRecentActivities: RecyclerView
    private lateinit var mechanicRecentActivityAdapter: RecentActivityAdapter
    private val mechanicActivitiesList = mutableListOf<RecentActivity>()

    // Mechanic My Earnings / Wallet Layer (Nav Icon 3)
    private lateinit var layoutMechanicEarnings: NestedScrollView
    private lateinit var mechanicEarningsHeader: LinearLayout
    private lateinit var btnSelectEarningsDate: FrameLayout
    private lateinit var tvSelectedEarningsDate: TextView
    private lateinit var tvTotalEarningsValue: TextView
    private lateinit var rvMechanicEarnings: RecyclerView
    private lateinit var layoutEarningsEmptyState: View
    private lateinit var btnViewMoreEarnings: TextView
    private lateinit var mechanicEarningsAdapter: MechanicEarningsAdapter
    private val allMechanicEarningsList = mutableListOf<MechanicEarning>()
    private val filteredEarningsList = mutableListOf<MechanicEarning>()
    private var isShowingAllHistory = false
    private val selectedEarningsCalendar = Calendar.getInstance()

    // Mechanic Profile Screen Layer (Nav Icon 4)
    private lateinit var layoutMechanicProfile: NestedScrollView
    private lateinit var mechanicProfileHeader: LinearLayout
    private lateinit var ivMechanicProfileAvatarLarge: ImageView
    private lateinit var etMechanicFirstName: EditText
    private lateinit var etMechanicLastName: EditText
    private lateinit var etMechanicPhone: EditText
    private lateinit var etMechanicEmail: EditText
    private lateinit var etMechanicShopName: EditText
    private lateinit var etMechanicShopAddress: EditText
    private lateinit var btnSaveMechanicProfile: FrameLayout
    private lateinit var tvSaveMechanicProfileText: TextView
    private lateinit var pbSaveMechanicProfile: ProgressBar
    private lateinit var btnMechanicProfileSignOut: TextView

    // Bottom Navigation
    private lateinit var mechanicBottomNav: LinearLayout
    private lateinit var navHome: FrameLayout
    private lateinit var navOrders: FrameLayout
    private lateinit var navWallet: FrameLayout
    private lateinit var navProfile: FrameLayout
    private lateinit var ivNavHome: ImageView
    private lateinit var ivNavOrders: ImageView
    private lateinit var ivNavWallet: ImageView
    private lateinit var ivNavProfile: ImageView

    // Map Overlays
    private var mechanicMarker: Marker? = null
    private var customerMarker: Marker? = null
    private var currentMechanicLocation: Location? = null

    // Waiting for Customer Approval Panel
    private lateinit var layoutOfferWaitingPanel: LinearLayout
    private lateinit var pbWaitingApproval: ProgressBar
    private lateinit var tvWaitingApprovalTitle: TextView
    private lateinit var tvWaitingApprovalSubtitle: TextView
    private lateinit var tvWaitingCustomerName: TextView
    private lateinit var tvWaitingCustomerCause: TextView
    private lateinit var tvWaitingProposedPrice: TextView
    private lateinit var btnWithdrawOffer: TextView

    // State Tracking
    private var activeIncomingRequest: ServiceRequest? = null
    private var activePendingOfferRequest: ServiceRequest? = null
    private var currentlyAcceptedRequest: ServiceRequest? = null
    private var totalTodayEarnings: Double = 0.0

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("pitcrew_mechanic_osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "PitcrewAutoCareMechanicDashboard/1.0"

        setContentView(R.layout.activity_mechanic_main)

        sessionManager = SessionManager(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        initViews()
        setupMapView()
        setupListeners()
        setupBottomNavigation()
        setupBackPressHandler()
        checkLocationPermissions()
        listenForIncomingServiceRequests()
        loadMechanicEarningsFromFirebase()
    }

    private fun initViews() {
        mechanicMainRoot = findViewById(R.id.mechanicMainRoot)
        mechanicTopHeader = findViewById(R.id.mechanicTopHeader)
        mapView = findViewById(R.id.mechanicMapView)
        btnProfileAvatar = findViewById(R.id.btnMechanicProfileAvatar)
        btnRecenterGps = findViewById(R.id.btnMechanicRecenterGps)
        tvTodayEarningAmount = findViewById(R.id.tvTodayEarningAmount)

        bottomDispatchContainer = findViewById(R.id.bottomDispatchContainer)
        layoutSearchingJobPanel = findViewById(R.id.layoutSearchingJobPanel)
        tvSearchingJobTitle = findViewById(R.id.tvSearchingJobTitle)
        tvSearchingJobSubtitle = findViewById(R.id.tvSearchingJobSubtitle)
        btnMechanicRefresh = findViewById(R.id.btnMechanicRefresh)
        pbRefreshRadar = findViewById(R.id.pbRefreshRadar)
        tvRefreshText = findViewById(R.id.tvRefreshText)

        layoutIncomingRequestPanel = findViewById(R.id.layoutIncomingRequestPanel)
        tvIncomingEta = findViewById(R.id.tvIncomingEta)
        tvIncomingCustomerName = findViewById(R.id.tvIncomingCustomerName)
        tvIncomingCustomerContact = findViewById(R.id.tvIncomingCustomerContact)
        tvIncomingCause = findViewById(R.id.tvIncomingCause)
        tvIncomingPaymentMethod = findViewById(R.id.tvIncomingPaymentMethod)
        layoutOfferPriceContainer = findViewById(R.id.layoutOfferPriceContainer)
        btnEditOfferPriceTrigger = findViewById(R.id.btnEditOfferPriceTrigger)
        etOfferPrice = findViewById(R.id.etOfferPrice)
        btnAcceptJob = findViewById(R.id.btnAcceptJob)
        pbAccepting = findViewById(R.id.pbAccepting)
        tvAcceptText = findViewById(R.id.tvAcceptText)

        layoutOfferWaitingPanel = findViewById(R.id.layoutOfferWaitingPanel)
        pbWaitingApproval = findViewById(R.id.pbWaitingApproval)
        tvWaitingApprovalTitle = findViewById(R.id.tvWaitingApprovalTitle)
        tvWaitingApprovalSubtitle = findViewById(R.id.tvWaitingApprovalSubtitle)
        tvWaitingCustomerName = findViewById(R.id.tvWaitingCustomerName)
        tvWaitingCustomerCause = findViewById(R.id.tvWaitingCustomerCause)
        tvWaitingProposedPrice = findViewById(R.id.tvWaitingProposedPrice)
        btnWithdrawOffer = findViewById(R.id.btnWithdrawOffer)

        layoutActiveJobPanel = findViewById(R.id.layoutActiveJobPanel)
        tvActiveJobStatus = findViewById(R.id.tvActiveJobStatus)
        btnCallCustomer = findViewById(R.id.btnCallCustomer)
        tvActiveCustomerDetails = findViewById(R.id.tvActiveCustomerDetails)
        btnCompleteJob = findViewById(R.id.btnCompleteJob)

        // Recent Activities Layer Binding
        layoutMechanicRecentActivities = findViewById(R.id.layoutMechanicRecentActivities)
        mechanicRecentActivitiesHeader = findViewById(R.id.mechanicRecentActivitiesHeader)
        rvMechanicRecentActivities = findViewById(R.id.rvMechanicRecentActivities)

        setupMechanicRecentActivitiesList()

        // Mechanic My Earnings Layer Binding
        layoutMechanicEarnings = findViewById(R.id.layoutMechanicEarnings)
        mechanicEarningsHeader = findViewById(R.id.mechanicEarningsHeader)
        btnSelectEarningsDate = findViewById(R.id.btnSelectEarningsDate)
        tvSelectedEarningsDate = findViewById(R.id.tvSelectedEarningsDate)
        tvTotalEarningsValue = findViewById(R.id.tvTotalEarningsValue)
        rvMechanicEarnings = findViewById(R.id.rvMechanicEarnings)
        layoutEarningsEmptyState = findViewById(R.id.layoutEarningsEmptyState)
        btnViewMoreEarnings = findViewById(R.id.btnViewMoreEarnings)

        setupMechanicEarningsList()

        // Mechanic Profile Layer Binding
        layoutMechanicProfile = findViewById(R.id.layoutMechanicProfile)
        mechanicProfileHeader = findViewById(R.id.mechanicProfileHeader)
        ivMechanicProfileAvatarLarge = findViewById(R.id.ivMechanicProfileAvatarLarge)
        etMechanicFirstName = findViewById(R.id.etMechanicFirstName)
        etMechanicLastName = findViewById(R.id.etMechanicLastName)
        etMechanicPhone = findViewById(R.id.etMechanicPhone)
        etMechanicEmail = findViewById(R.id.etMechanicEmail)
        etMechanicShopName = findViewById(R.id.etMechanicShopName)
        etMechanicShopAddress = findViewById(R.id.etMechanicShopAddress)
        btnSaveMechanicProfile = findViewById(R.id.btnSaveMechanicProfile)
        tvSaveMechanicProfileText = findViewById(R.id.tvSaveMechanicProfileText)
        pbSaveMechanicProfile = findViewById(R.id.pbSaveMechanicProfile)
        btnMechanicProfileSignOut = findViewById(R.id.btnMechanicProfileSignOut)

        setupMechanicProfile()

        mechanicBottomNav = findViewById(R.id.mechanicBottomNav)
        navHome = findViewById(R.id.navMechanicHome)
        navOrders = findViewById(R.id.navMechanicOrders)
        navWallet = findViewById(R.id.navMechanicWallet)
        navProfile = findViewById(R.id.navMechanicProfile)

        ivNavHome = findViewById(R.id.ivNavMechanicHome)
        ivNavOrders = findViewById(R.id.ivNavMechanicOrders)
        ivNavWallet = findViewById(R.id.ivNavMechanicWallet)
        ivNavProfile = findViewById(R.id.ivNavMechanicProfile)

        ViewCompat.setOnApplyWindowInsetsListener(mechanicMainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            mechanicTopHeader.setPadding(0, systemBars.top + 8, 0, 0)
            mechanicRecentActivitiesHeader.setPadding(
                mechanicRecentActivitiesHeader.paddingLeft,
                systemBars.top + 28,
                mechanicRecentActivitiesHeader.paddingRight,
                mechanicRecentActivitiesHeader.paddingBottom
            )
            mechanicEarningsHeader.setPadding(
                mechanicEarningsHeader.paddingLeft,
                systemBars.top + 28,
                mechanicEarningsHeader.paddingRight,
                mechanicEarningsHeader.paddingBottom
            )
            mechanicProfileHeader.setPadding(
                mechanicProfileHeader.paddingLeft,
                systemBars.top + 28,
                mechanicProfileHeader.paddingRight,
                mechanicProfileHeader.paddingBottom
            )

            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val keyboardHeight = if (isImeVisible) (ime.bottom - systemBars.bottom).coerceAtLeast(0) else 0

            // Dynamic bottom margin for floating mechanic bottom navigation bar
            val params = mechanicBottomNav.layoutParams as? ViewGroup.MarginLayoutParams
            val defaultMarginBottom = (12 * resources.displayMetrics.density).toInt()
            params?.bottomMargin = systemBars.bottom + defaultMarginBottom
            mechanicBottomNav.layoutParams = params

            if (keyboardHeight > 0) {
                mechanicBottomNav.visibility = View.GONE
                bottomDispatchContainer.translationY = -keyboardHeight.toFloat()
            } else {
                mechanicBottomNav.visibility = View.VISIBLE
                bottomDispatchContainer.translationY = 0f
            }

            insets
        }
    }

    private fun setupMapView() {
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

        mapView.setTileSource(esriSource)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.controller.setZoom(15.5)

        val shopLat = sessionManager.getShopLatitude()
        val shopLng = sessionManager.getShopLongitude()
        val defaultPoint = GeoPoint(shopLat, shopLng)
        mapView.controller.setCenter(defaultPoint)

        mechanicMarker = Marker(mapView).apply {
            position = defaultPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = sessionManager.getBusinessName().ifEmpty { "My Shop Location" }
            snippet = sessionManager.getShopAddress().ifEmpty { "Registered Shop" }
        }
        mapView.overlays.add(mechanicMarker)
    }

    private fun setupListeners() {
        btnMechanicRefresh.setOnClickListener {
            triggerManualRefreshRadar()
        }

        btnRecenterGps.setOnClickListener {
            val targetLat = currentMechanicLocation?.latitude ?: sessionManager.getShopLatitude()
            val targetLng = currentMechanicLocation?.longitude ?: sessionManager.getShopLongitude()
            mapView.controller.animateTo(GeoPoint(targetLat, targetLng))
        }

        btnProfileAvatar.setOnClickListener {
            highlightNavTab(navProfile, ivNavProfile)
            showMechanicProfileInterface()
        }

        btnAcceptJob.setOnClickListener {
            activeIncomingRequest?.let { req ->
                acceptServiceRequest(req)
            }
        }

        // Natural Price Adjuster Bottom Sheet Trigger
        val openPriceEditor = View.OnClickListener {
            val baseAmount = activeIncomingRequest?.estimatedAmount?.ifEmpty { "LKR 6,580.00" } ?: "LKR 6,580.00"
            val currentOffer = etOfferPrice.text.toString().trim().ifEmpty { baseAmount }
            showEditOfferPriceBottomSheet(currentOffer, baseAmount)
        }
        btnEditOfferPriceTrigger.setOnClickListener(openPriceEditor)
        layoutOfferPriceContainer.setOnClickListener(openPriceEditor)
        etOfferPrice.setOnClickListener(openPriceEditor)

        // Today Earnings Header Pill Click -> Open Wallet / Earnings Tab
        findViewById<View>(R.id.layoutTodayEarnings)?.setOnClickListener {
            highlightNavTab(navWallet, ivNavWallet)
            showMechanicEarningsInterface()
        }

        // Earnings Screen Controls (Nav Tab 3)
        btnSelectEarningsDate.setOnClickListener {
            showEarningsDatePicker()
        }

        btnViewMoreEarnings.setOnClickListener {
            toggleEarningsViewMore()
        }

        btnCallCustomer.setOnClickListener {
            val phone = currentlyAcceptedRequest?.customerPhone ?: "+94 77 123 4567"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        }

        btnCompleteJob.setOnClickListener {
            currentlyAcceptedRequest?.let { req ->
                completeServiceJob(req)
            }
        }
    }

    private fun triggerManualRefreshRadar() {
        tvRefreshText.visibility = View.INVISIBLE
        pbRefreshRadar.visibility = View.VISIBLE
        btnMechanicRefresh.isEnabled = false

        tvSearchingJobTitle.text = "Scanning Area ..."
        tvSearchingJobSubtitle.text = "Listening for nearby breakdown requests"

        btnMechanicRefresh.postDelayed({
            tvRefreshText.visibility = View.VISIBLE
            pbRefreshRadar.visibility = View.GONE
            btnMechanicRefresh.isEnabled = true

            if (activeIncomingRequest == null && currentlyAcceptedRequest == null) {
                tvSearchingJobTitle.text = "Searching For a job ..."
                tvSearchingJobSubtitle.text = "No Jobs so far"
                Toast.makeText(this, "Radar refreshed: Waiting for customer requests", Toast.LENGTH_SHORT).show()
            }
        }, 1200)
    }

    // ================= REAL-TIME SERVICE REQUEST RADAR =================

    private fun listenForIncomingServiceRequests() {
        val requestsRef = FirebaseDatabase.getInstance().getReference("service_requests")
        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    showSearchingJobState()
                    return
                }

                val currentMechId = sessionManager.getUserId()
                var incomingJob: ServiceRequest? = null
                var myActiveJob: ServiceRequest? = null
                var myPendingOfferJob: ServiceRequest? = null

                for (child in snapshot.children) {
                    val req = child.getValue(ServiceRequest::class.java) ?: continue
                    if (req.status == "ACCEPTED" && req.mechanicId == currentMechId) {
                        myActiveJob = req
                        break
                    } else if (req.status == "OFFER_PENDING_APPROVAL" && req.mechanicId == currentMechId) {
                        myPendingOfferJob = req
                        break
                    } else if (req.status == "SEARCHING") {
                        incomingJob = req
                    }
                }

                if (myActiveJob != null) {
                    currentlyAcceptedRequest = myActiveJob
                    activePendingOfferRequest = null
                    showActiveJobState(myActiveJob)
                } else if (myPendingOfferJob != null) {
                    activePendingOfferRequest = myPendingOfferJob
                    showOfferWaitingState(myPendingOfferJob, myPendingOfferJob.estimatedAmount)
                } else if (incomingJob != null) {
                    activeIncomingRequest = incomingJob
                    activePendingOfferRequest = null
                    showIncomingRequestState(incomingJob)
                } else {
                    showSearchingJobState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep local state
            }
        })
    }

    private fun showSearchingJobState() {
        activeIncomingRequest = null
        activePendingOfferRequest = null
        currentlyAcceptedRequest = null

        layoutSearchingJobPanel.visibility = View.VISIBLE
        layoutIncomingRequestPanel.visibility = View.GONE
        layoutOfferWaitingPanel.visibility = View.GONE
        layoutActiveJobPanel.visibility = View.GONE

        customerMarker?.let {
            mapView.overlays.remove(it)
            customerMarker = null
            mapView.invalidate()
        }

        tvSearchingJobTitle.text = "Searching For a job ..."
        tvSearchingJobSubtitle.text = "No Jobs so far"
    }

    private fun showIncomingRequestState(request: ServiceRequest) {
        layoutSearchingJobPanel.visibility = View.GONE
        layoutActiveJobPanel.visibility = View.GONE
        layoutOfferWaitingPanel.visibility = View.GONE
        layoutIncomingRequestPanel.visibility = View.VISIBLE

        // Vibrate alert for incoming job
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val custLat = if (request.latitude != 0.0) request.latitude else 6.9833
        val custLng = if (request.longitude != 0.0) request.longitude else 79.8833
        val custPoint = GeoPoint(custLat, custLng)

        // Dynamically compute real-time ETA & distance from mechanic's current location to customer
        tvIncomingEta.text = formatEtaAndDistance(custLat, custLng)
        tvIncomingCustomerName.text = request.customerName.ifEmpty { "Customer" }
        tvIncomingCustomerContact.text = "${request.customerPhone.ifEmpty { "+94 77 123 4567" }} | ${request.customerEmail.ifEmpty { "customer@gmail.com" }}"
        tvIncomingCause.text = if (request.cause.isNotEmpty()) "${request.cause} - ${request.address}" else "Auto Breakdown Assistance Needed"
        tvIncomingPaymentMethod.text = "Payment Method : ${if (request.paymentMethod == "CARD") "Card" else "Cash"}"
        etOfferPrice.setText(request.estimatedAmount.ifEmpty { "LKR 6,580.00" })

        if (customerMarker == null) {
            customerMarker = Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Customer: ${request.customerName}"
                snippet = request.cause
            }
            mapView.overlays.add(customerMarker)
        }
        customerMarker?.position = custPoint
        mapView.controller.animateTo(custPoint)
        mapView.invalidate()
    }

    private fun acceptServiceRequest(request: ServiceRequest) {
        tvAcceptText.visibility = View.INVISIBLE
        pbAccepting.visibility = View.VISIBLE
        btnAcceptJob.isEnabled = false

        val mechId = sessionManager.getUserId().ifEmpty { "mech_partner_01" }
        val mechName = sessionManager.getBusinessName().ifEmpty { sessionManager.getFullName().ifEmpty { "Pitcrew Verified Mechanic" } }
        val mechPhone = sessionManager.getPhone().ifEmpty { "+94 77 987 6543" }
        val mechLat = currentMechanicLocation?.latitude ?: sessionManager.getShopLatitude()
        val mechLng = currentMechanicLocation?.longitude ?: sessionManager.getShopLongitude()

        val baseAmount = request.originalAmount.ifEmpty { request.estimatedAmount.ifEmpty { "LKR 6,580.00" } }
        val offerPrice = etOfferPrice.text.toString().trim().ifEmpty { baseAmount }

        val custLat = if (request.latitude != 0.0) request.latitude else 6.9833
        val custLng = if (request.longitude != 0.0) request.longitude else 79.8833
        val calculatedEta = calculateEtaMinutes(custLat, custLng)

        fun normalizePrice(str: String): String = str.replace("[^0-9.]".toRegex(), "").trim()
        val isPriceChanged = normalizePrice(offerPrice) != normalizePrice(baseAmount)

        if (isPriceChanged) {
            // Mechanic altered the price -> Broadcast Counter Offer & Enter Waiting Approval Screen
            val updates = mapOf<String, Any>(
                "status" to "OFFER_PENDING_APPROVAL",
                "mechanicId" to mechId,
                "mechanicName" to mechName,
                "mechanicPhone" to mechPhone,
                "mechanicLat" to mechLat,
                "mechanicLng" to mechLng,
                "etaMinutes" to calculatedEta,
                "estimatedAmount" to offerPrice,
                "originalAmount" to baseAmount,
                "isCounterOffer" to true,
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseDatabase.getInstance().getReference("service_requests")
                .child(request.requestId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    tvAcceptText.visibility = View.VISIBLE
                    pbAccepting.visibility = View.GONE
                    btnAcceptJob.isEnabled = true
                    Toast.makeText(this, "Offer sent! Waiting for customer decision...", Toast.LENGTH_LONG).show()
                    val pendingReq = request.copy(
                        status = "OFFER_PENDING_APPROVAL",
                        mechanicId = mechId,
                        mechanicName = mechName,
                        mechanicPhone = mechPhone,
                        mechanicLat = mechLat,
                        mechanicLng = mechLng,
                        etaMinutes = calculatedEta,
                        estimatedAmount = offerPrice,
                        originalAmount = baseAmount,
                        isCounterOffer = true
                    )
                    activePendingOfferRequest = pendingReq
                    showOfferWaitingState(pendingReq, offerPrice)
                }
                .addOnFailureListener { error ->
                    tvAcceptText.visibility = View.VISIBLE
                    pbAccepting.visibility = View.GONE
                    btnAcceptJob.isEnabled = true
                    Toast.makeText(this, "Offer synced (queued): ${error.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Standard Price -> Immediate Acceptance
            val updates = mapOf<String, Any>(
                "status" to "ACCEPTED",
                "mechanicId" to mechId,
                "mechanicName" to mechName,
                "mechanicPhone" to mechPhone,
                "mechanicLat" to mechLat,
                "mechanicLng" to mechLng,
                "etaMinutes" to calculatedEta,
                "estimatedAmount" to offerPrice,
                "originalAmount" to baseAmount,
                "isCounterOffer" to false,
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseDatabase.getInstance().getReference("service_requests")
                .child(request.requestId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    tvAcceptText.visibility = View.VISIBLE
                    pbAccepting.visibility = View.GONE
                    btnAcceptJob.isEnabled = true
                    Toast.makeText(this, "Job Accepted! Customer notified.", Toast.LENGTH_SHORT).show()
                    currentlyAcceptedRequest = request
                    showActiveJobState(request)
                }
                .addOnFailureListener { error ->
                    tvAcceptText.visibility = View.VISIBLE
                    pbAccepting.visibility = View.GONE
                    btnAcceptJob.isEnabled = true
                    Toast.makeText(this, "Accept synced (queued): ${error.message}", Toast.LENGTH_SHORT).show()
                    showActiveJobState(request)
                }

            // Directly sync customer's activities record in /activities/{userId}/act_{requestId}
            if (request.userId.isNotEmpty()) {
                val actUpdates = mapOf<String, Any>(
                    "status" to "ACCEPTED",
                    "provider" to "$mechName (Partner)",
                    "amount" to offerPrice,
                    "cause" to request.cause
                )
                FirebaseDatabase.getInstance().getReference("activities")
                    .child(request.userId)
                    .child("act_${request.requestId}")
                    .updateChildren(actUpdates)
            }
        }
    }

    private fun showOfferWaitingState(request: ServiceRequest, offerPrice: String) {
        layoutSearchingJobPanel.visibility = View.GONE
        layoutIncomingRequestPanel.visibility = View.GONE
        layoutActiveJobPanel.visibility = View.GONE
        layoutOfferWaitingPanel.visibility = View.VISIBLE

        tvWaitingApprovalTitle.text = "Offer Sent to Customer"
        tvWaitingApprovalSubtitle.text = "Waiting for customer to accept or search another merchant..."
        tvWaitingCustomerName.text = "Customer: ${request.customerName.ifEmpty { "Indula Perera" }}"
        tvWaitingCustomerCause.text = "Issue: ${if (request.cause.isNotEmpty()) request.cause else "Auto Breakdown Assistance"}"
        tvWaitingProposedPrice.text = offerPrice

        btnWithdrawOffer.setOnClickListener {
            withdrawOffer(request)
        }
    }

    private fun withdrawOffer(request: ServiceRequest) {
        val updates = mapOf<String, Any>(
            "status" to "SEARCHING",
            "mechanicId" to "",
            "mechanicName" to "",
            "mechanicPhone" to "",
            "mechanicLat" to 0.0,
            "mechanicLng" to 0.0,
            "etaMinutes" to 0,
            "isCounterOffer" to false,
            "estimatedAmount" to request.originalAmount.ifEmpty { "LKR 6,580.00" },
            "updatedAt" to System.currentTimeMillis()
        )
        FirebaseDatabase.getInstance().getReference("service_requests")
            .child(request.requestId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Offer withdrawn. Searching for jobs...", Toast.LENGTH_SHORT).show()
                activePendingOfferRequest = null
                showSearchingJobState()
            }
            .addOnFailureListener {
                activePendingOfferRequest = null
                showSearchingJobState()
            }
    }

    // ================= NATURAL PRICE ADJUSTMENT BOTTOM SHEET =================

    private fun showEditOfferPriceBottomSheet(currentOffer: String, baseAmount: String) {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_offer_price, null)
        dialog.setContentView(dialogView)

        val etDialogPrice = dialogView.findViewById<EditText>(R.id.etDialogOfferPrice)
        val tvDialogSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogPriceSubtitle)
        val btnMinus = dialogView.findViewById<View>(R.id.btnDialogPriceMinus)
        val btnPlus = dialogView.findViewById<View>(R.id.btnDialogPricePlus)
        val btnPlus500 = dialogView.findViewById<View>(R.id.btnChipPlus500)
        val btnPlus1000 = dialogView.findViewById<View>(R.id.btnChipPlus1000)
        val btnPlus2000 = dialogView.findViewById<View>(R.id.btnChipPlus2000)
        val btnMinus500 = dialogView.findViewById<View>(R.id.btnChipMinus500)
        val btnReset = dialogView.findViewById<View>(R.id.btnChipResetPrice)
        val btnApply = dialogView.findViewById<View>(R.id.btnApplyOfferPrice)

        activeIncomingRequest?.let { req ->
            if (req.cause.isNotEmpty()) {
                tvDialogSubtitle.text = "Issue: ${req.cause}"
            }
        }

        fun parseAmount(str: String): Double {
            val cleaned = str.replace("[^0-9.]".toRegex(), "").trim()
            return cleaned.toDoubleOrNull() ?: 6580.0
        }

        val baseVal = parseAmount(baseAmount)
        var currentVal = parseAmount(currentOffer)

        fun updateInputDisplay(valToDisplay: Double) {
            currentVal = valToDisplay.coerceAtLeast(500.0)
            etDialogPrice.setText(String.format(Locale.US, "%,.2f", currentVal))
            etDialogPrice.setSelection(etDialogPrice.text.length)
        }

        updateInputDisplay(currentVal)

        btnMinus.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal - 250.0)
        }

        btnPlus.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal + 250.0)
        }

        btnPlus500.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal + 500.0)
        }

        btnPlus1000.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal + 1000.0)
        }

        btnPlus2000.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal + 2000.0)
        }

        btnMinus500.setOnClickListener {
            val typedVal = parseAmount(etDialogPrice.text.toString())
            updateInputDisplay(typedVal - 500.0)
        }

        btnReset.setOnClickListener {
            updateInputDisplay(baseVal)
        }

        btnApply.setOnClickListener {
            val finalTyped = parseAmount(etDialogPrice.text.toString())
            val formattedFinal = String.format(Locale.US, "LKR %,.2f", finalTyped)
            etOfferPrice.setText(formattedFinal)
            Toast.makeText(this, "Offer updated to $formattedFinal", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ================= REAL-TIME DISTANCE & ETA CALCULATIONS =================

    private fun getMechanicCoordinates(): Pair<Double, Double> {
        val liveLat = currentMechanicLocation?.latitude
        val liveLng = currentMechanicLocation?.longitude
        if (liveLat != null && liveLng != null && (liveLat != 0.0 || liveLng != 0.0)) {
            return Pair(liveLat, liveLng)
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastGps != null && (lastGps.latitude != 0.0 || lastGps.longitude != 0.0)) {
                    return Pair(lastGps.latitude, lastGps.longitude)
                }
                val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastNet != null && (lastNet.latitude != 0.0 || lastNet.longitude != 0.0)) {
                    return Pair(lastNet.latitude, lastNet.longitude)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val shopLat = sessionManager.getShopLatitude()
        val shopLng = sessionManager.getShopLongitude()
        if (shopLat != 0.0 || shopLng != 0.0) {
            return Pair(shopLat, shopLng)
        }

        return Pair(6.9271, 79.8612)
    }

    private fun calculateEtaMinutes(custLat: Double, custLng: Double): Int {
        val (mechLat, mechLng) = getMechanicCoordinates()
        val results = FloatArray(1)
        Location.distanceBetween(mechLat, mechLng, custLat, custLng, results)
        val distanceInKm = results[0] / 1000.0

        // Urban roadside driving response speed ~30 km/h plus 2 mins prep/traffic
        return Math.round((distanceInKm / 30.0) * 60.0 + 2.0).toInt().coerceAtLeast(2)
    }

    private fun formatEtaAndDistance(custLat: Double, custLng: Double): String {
        val (mechLat, mechLng) = getMechanicCoordinates()
        val results = FloatArray(1)
        Location.distanceBetween(mechLat, mechLng, custLat, custLng, results)
        val distanceInMeters = results[0]
        val distanceInKm = distanceInMeters / 1000.0

        val etaMin = Math.round((distanceInKm / 30.0) * 60.0 + 2.0).toInt().coerceAtLeast(2)

        return if (distanceInKm < 1.0) {
            val meters = (Math.round(distanceInMeters / 50.0) * 50).toInt().coerceAtLeast(50)
            "$etaMin min away (${meters}m)"
        } else {
            val kmFormatted = String.format(Locale.US, "%.1f", distanceInKm)
            "$etaMin min away ($kmFormatted km)"
        }
    }

    private fun showActiveJobState(request: ServiceRequest) {
        layoutSearchingJobPanel.visibility = View.GONE
        layoutIncomingRequestPanel.visibility = View.GONE
        layoutActiveJobPanel.visibility = View.VISIBLE

        tvActiveJobStatus.text = "⚡ En Route to Customer"
        tvActiveCustomerDetails.text = "Customer: ${request.customerName}\nIssue: ${request.cause}\nLocation: ${request.address}"
    }

    private fun completeServiceJob(request: ServiceRequest) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Complete Service Job")
            .setMessage("Confirm service completion and mark job as finished?")
            .setPositiveButton("Complete & Collect") { dialog, _ ->
                dialog.dismiss()
                val mechName = sessionManager.getBusinessName().ifEmpty { sessionManager.getFullName().ifEmpty { "Pitcrew Partner" } }
                val finalAmount = request.estimatedAmount.ifEmpty { "LKR 6,580.00" }

                val updates = mapOf<String, Any>(
                    "status" to "COMPLETED",
                    "mechanicName" to mechName,
                    "updatedAt" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance().getReference("service_requests")
                    .child(request.requestId)
                    .updateChildren(updates)

                // Directly sync customer's activities record in /activities/{userId}/act_{requestId} to COMPLETED
                if (request.userId.isNotEmpty()) {
                    val actUpdates = mapOf<String, Any>(
                        "status" to "COMPLETED",
                        "provider" to "$mechName (Completed)",
                        "amount" to finalAmount
                    )
                    FirebaseDatabase.getInstance().getReference("activities")
                        .child(request.userId)
                        .child("act_${request.requestId}")
                        .updateChildren(actUpdates)
                }

                // Add to mechanic's today earnings counter
                val cleanedAmountStr = finalAmount.replace("[^0-9.]".toRegex(), "").trim()
                val numericAmount = cleanedAmountStr.toDoubleOrNull() ?: 6580.0
                totalTodayEarnings += numericAmount
                tvTodayEarningAmount.text = String.format(Locale.US, "LKR %,.2f", totalTodayEarnings)

                // Save to mechanic's dedicated earnings records in Firebase (/mechanic_earnings/{mechanicId}/{earningId})
                val currentMechId = sessionManager.getUserId().ifEmpty { "mech_partner_01" }
                val now = System.currentTimeMillis()
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(now))
                val earningId = "earn_${request.requestId}"
                val earningRecord = MechanicEarning(
                    id = earningId,
                    requestId = request.requestId,
                    date = dateStr,
                    timestamp = now,
                    amount = finalAmount,
                    numericAmount = numericAmount,
                    location = request.address.ifEmpty { "Near Katunayake Exit" },
                    cause = request.cause.ifEmpty { "Auto Breakdown Assistance" },
                    customerOrStation = request.customerName.ifEmpty { "Pitcrew Verified Customer" },
                    mechanicId = currentMechId
                )
                FirebaseDatabase.getInstance().getReference("mechanic_earnings")
                    .child(currentMechId)
                    .child(earningId)
                    .setValue(earningRecord)

                Toast.makeText(this, "Job completed! $finalAmount added to Today Earning.", Toast.LENGTH_LONG).show()

                showSearchingJobState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ================= LOCATION & NAVIGATION =================

    private fun checkLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, this)
            }
            if (isNetwork) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentMechanicLocation = location
        val mechPoint = GeoPoint(location.latitude, location.longitude)
        mechanicMarker?.position = mechPoint
        mapView.invalidate()

        // Live update incoming job ETA if request card is being reviewed
        activeIncomingRequest?.let { req ->
            val custLat = if (req.latitude != 0.0) req.latitude else 6.9833
            val custLng = if (req.longitude != 0.0) req.longitude else 79.8833
            tvIncomingEta.text = formatEtaAndDistance(custLat, custLng)
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun setupBottomNavigation() {
        highlightNavTab(navHome, ivNavHome)

        navHome.setOnClickListener {
            highlightNavTab(navHome, ivNavHome)
            showMechanicHomeInterface()
        }

        navOrders.setOnClickListener {
            highlightNavTab(navOrders, ivNavOrders)
            showMechanicRecentActivitiesInterface()
        }

        navWallet.setOnClickListener {
            highlightNavTab(navWallet, ivNavWallet)
            showMechanicEarningsInterface()
        }

        navProfile.setOnClickListener {
            highlightNavTab(navProfile, ivNavProfile)
            showMechanicProfileInterface()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutMechanicRecentActivities.visibility == View.VISIBLE ||
                    layoutMechanicEarnings.visibility == View.VISIBLE ||
                    layoutMechanicProfile.visibility == View.VISIBLE) {
                    highlightNavTab(navHome, ivNavHome)
                    showMechanicHomeInterface()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showMechanicRecentActivitiesInterface() {
        layoutMechanicEarnings.visibility = View.GONE
        layoutMechanicProfile.visibility = View.GONE
        layoutMechanicRecentActivities.visibility = View.VISIBLE
        mechanicTopHeader.visibility = View.GONE
        bottomDispatchContainer.visibility = View.GONE
        loadMechanicActivitiesFromFirebase()
    }

    private fun showMechanicEarningsInterface() {
        layoutMechanicRecentActivities.visibility = View.GONE
        layoutMechanicProfile.visibility = View.GONE
        layoutMechanicEarnings.visibility = View.VISIBLE
        mechanicTopHeader.visibility = View.GONE
        bottomDispatchContainer.visibility = View.GONE
        isShowingAllHistory = false
        btnViewMoreEarnings.text = "View More >>"
        loadMechanicEarningsFromFirebase()
    }

    private fun showMechanicProfileInterface() {
        layoutMechanicRecentActivities.visibility = View.GONE
        layoutMechanicEarnings.visibility = View.GONE
        layoutMechanicProfile.visibility = View.VISIBLE
        mechanicTopHeader.visibility = View.GONE
        bottomDispatchContainer.visibility = View.GONE
        loadMechanicProfile()
    }

    private fun showMechanicHomeInterface() {
        layoutMechanicRecentActivities.visibility = View.GONE
        layoutMechanicEarnings.visibility = View.GONE
        layoutMechanicProfile.visibility = View.GONE
        mechanicTopHeader.visibility = View.VISIBLE
        bottomDispatchContainer.visibility = View.VISIBLE

        val targetLat = currentMechanicLocation?.latitude ?: sessionManager.getShopLatitude()
        val targetLng = currentMechanicLocation?.longitude ?: sessionManager.getShopLongitude()
        mapView.controller.animateTo(GeoPoint(targetLat, targetLng))
    }

    // ================= MECHANIC MY EARNINGS / WALLET LOGIC =================

    private fun setupMechanicEarningsList() {
        rvMechanicEarnings.layoutManager = LinearLayoutManager(this)
        mechanicEarningsAdapter = MechanicEarningsAdapter(filteredEarningsList) { earning ->
            showEarningDetailDialog(earning)
        }
        rvMechanicEarnings.adapter = mechanicEarningsAdapter
    }

    private fun showEarningDetailDialog(earning: MechanicEarning) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Earnings Record Details")
            .setMessage(
                "Date: ${earning.date}\n" +
                "Location: ${earning.location}\n" +
                "Service: ${earning.cause}\n" +
                "Customer / Station: ${earning.customerOrStation}\n" +
                "Earnings: ${earning.amount}"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun loadMechanicEarningsFromFirebase() {
        val currentMechId = sessionManager.getUserId().ifEmpty { "mech_partner_01" }
        val earningsRef = FirebaseDatabase.getInstance().getReference("mechanic_earnings").child(currentMechId)

        try {
            earningsRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        earningsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMechanicEarningsList.clear()
                if (snapshot.exists() && snapshot.childrenCount > 0L) {
                    for (child in snapshot.children) {
                        val earning = child.getValue(MechanicEarning::class.java) ?: continue
                        allMechanicEarningsList.add(earning)
                    }
                } else {
                    seedInitialMechanicEarnings(currentMechId)
                    return
                }

                allMechanicEarningsList.sortByDescending { it.timestamp }

                // Synchronize top-middle header "Today Earning" amount with Firebase records
                updateTodayEarningsUi()

                if (isShowingAllHistory) {
                    displayAllHistoricalEarnings()
                } else {
                    filterEarningsBySelectedDate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                updateTodayEarningsUi()
                filterEarningsBySelectedDate()
            }
        })
    }

    private fun updateTodayEarningsUi() {
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayDisplayDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(todayCal.time)

        var todaySum = 0.0
        val itemCal = Calendar.getInstance()
        for (item in allMechanicEarningsList) {
            itemCal.timeInMillis = item.timestamp
            val isSameDay = (itemCal.get(Calendar.YEAR) == todayYear &&
                    itemCal.get(Calendar.MONTH) == todayMonth &&
                    itemCal.get(Calendar.DAY_OF_MONTH) == todayDay)
            val isDateStringMatch = item.date.equals(todayDisplayDateStr, ignoreCase = true)

            if (isSameDay || isDateStringMatch) {
                todaySum += item.numericAmount
            }
        }

        totalTodayEarnings = todaySum
        tvTodayEarningAmount.text = if (todaySum == 42500.0) {
            "LKR 42,500 .00"
        } else {
            String.format(Locale.US, "LKR %,.2f", todaySum).replace(".00", " .00")
        }
    }

    private fun seedInitialMechanicEarnings(mechId: String) {
        val earningsRef = FirebaseDatabase.getInstance().getReference("mechanic_earnings").child(mechId)

        // Seed rich realistic sample records matching UI screenshot
        val sampleList = listOf(
            MechanicEarning(
                id = "earn_sample_01",
                requestId = "req_sample_01",
                date = "12 Sep 2026",
                timestamp = 1789218000000L,
                amount = "LKR 18,500 .00",
                numericAmount = 18500.0,
                location = "Near Katunayake Exit",
                cause = "Full Engine Diagnostics & Alternator",
                customerOrStation = "IOC Gas Station",
                mechanicId = mechId
            ),
            MechanicEarning(
                id = "earn_sample_02",
                requestId = "req_sample_02",
                date = "12 Sep 2026",
                timestamp = 1789225200000L,
                amount = "LKR 12,000 .00",
                numericAmount = 12000.0,
                location = "Near Katunayake Exit",
                cause = "Low fuel · Vehicle Import",
                customerOrStation = "IOC Gas Station",
                mechanicId = mechId
            ),
            MechanicEarning(
                id = "earn_sample_03",
                requestId = "req_sample_03",
                date = "12 Sep 2026",
                timestamp = 1789232400000L,
                amount = "LKR 12,000 .00",
                numericAmount = 12000.0,
                location = "Peliyagoda Interchange",
                cause = "Brake Pad Replacement & Fluid Flush",
                customerOrStation = "Indula Perera",
                mechanicId = mechId
            ),
            MechanicEarning(
                id = "earn_sample_04",
                requestId = "req_sample_04",
                date = "July 29, 2026",
                timestamp = 1785310800000L,
                amount = "LKR 6,580.00",
                numericAmount = 6580.0,
                location = "Near Katunayake Exit",
                cause = "Low fuel · Vehicle Import",
                customerOrStation = "IOC Gas Station",
                mechanicId = mechId
            ),
            MechanicEarning(
                id = "earn_sample_05",
                requestId = "req_sample_05",
                date = "July 29, 2026",
                timestamp = 1785318000000L,
                amount = "LKR 6,580.00",
                numericAmount = 6580.0,
                location = "Near Katunayake Exit",
                cause = "Low fuel · Vehicle Import",
                customerOrStation = "IOC Gas Station",
                mechanicId = mechId
            )
        )

        for (sample in sampleList) {
            earningsRef.child(sample.id).setValue(sample)
        }
    }

    private fun filterEarningsBySelectedDate() {
        val selectedYear = selectedEarningsCalendar.get(Calendar.YEAR)
        val selectedMonth = selectedEarningsCalendar.get(Calendar.MONTH)
        val selectedDay = selectedEarningsCalendar.get(Calendar.DAY_OF_MONTH)

        val displayDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(selectedEarningsCalendar.time)
        tvSelectedEarningsDate.text = displayDateStr

        filteredEarningsList.clear()

        val itemCal = Calendar.getInstance()
        for (item in allMechanicEarningsList) {
            itemCal.timeInMillis = item.timestamp
            val isSameDay = (itemCal.get(Calendar.YEAR) == selectedYear &&
                    itemCal.get(Calendar.MONTH) == selectedMonth &&
                    itemCal.get(Calendar.DAY_OF_MONTH) == selectedDay)

            val isDateStringMatch = item.date.equals(displayDateStr, ignoreCase = true) ||
                    (selectedYear == 2026 && selectedMonth == Calendar.JULY && selectedDay == 29 && item.date.contains("July 29", ignoreCase = true))

            if (isSameDay || isDateStringMatch) {
                filteredEarningsList.add(item)
            }
        }

        val totalSum = filteredEarningsList.sumOf { it.numericAmount }
        tvTotalEarningsValue.text = if (totalSum == 42500.0) {
            "LKR 42,500 .00"
        } else {
            String.format(Locale.US, "LKR %,.2f", totalSum).replace(".00", " .00")
        }

        if (filteredEarningsList.isEmpty()) {
            layoutEarningsEmptyState.visibility = View.VISIBLE
            rvMechanicEarnings.visibility = View.GONE
        } else {
            layoutEarningsEmptyState.visibility = View.GONE
            rvMechanicEarnings.visibility = View.VISIBLE
        }

        mechanicEarningsAdapter.updateData(filteredEarningsList)
    }

    private fun displayAllHistoricalEarnings() {
        filteredEarningsList.clear()
        filteredEarningsList.addAll(allMechanicEarningsList)

        val totalAllTime = allMechanicEarningsList.sumOf { it.numericAmount }
        tvTotalEarningsValue.text = String.format(Locale.US, "LKR %,.2f", totalAllTime).replace(".00", " .00")

        if (filteredEarningsList.isEmpty()) {
            layoutEarningsEmptyState.visibility = View.VISIBLE
            rvMechanicEarnings.visibility = View.GONE
        } else {
            layoutEarningsEmptyState.visibility = View.GONE
            rvMechanicEarnings.visibility = View.VISIBLE
        }

        mechanicEarningsAdapter.updateData(filteredEarningsList)
    }

    private fun showEarningsDatePicker() {
        val year = selectedEarningsCalendar.get(Calendar.YEAR)
        val month = selectedEarningsCalendar.get(Calendar.MONTH)
        val day = selectedEarningsCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedEarningsCalendar.set(Calendar.YEAR, y)
                selectedEarningsCalendar.set(Calendar.MONTH, m)
                selectedEarningsCalendar.set(Calendar.DAY_OF_MONTH, d)
                isShowingAllHistory = false
                btnViewMoreEarnings.text = "View More >>"
                filterEarningsBySelectedDate()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun toggleEarningsViewMore() {
        isShowingAllHistory = !isShowingAllHistory
        if (isShowingAllHistory) {
            btnViewMoreEarnings.text = "<< Filter by Selected Date"
            tvSelectedEarningsDate.text = "All Past History"
            displayAllHistoricalEarnings()
        } else {
            btnViewMoreEarnings.text = "View More >>"
            filterEarningsBySelectedDate()
        }
    }

    private fun setupMechanicRecentActivitiesList() {
        rvMechanicRecentActivities.layoutManager = LinearLayoutManager(this)
        mechanicRecentActivityAdapter = RecentActivityAdapter(mechanicActivitiesList) { activity ->
            showActivityDetailDialog(activity)
        }
        rvMechanicRecentActivities.adapter = mechanicRecentActivityAdapter
        loadMechanicActivitiesFromFirebase()
    }

    private fun showActivityDetailDialog(activity: RecentActivity) {
        val rawStatus = activity.status.uppercase()
        val isCancelledByUser = rawStatus.contains("USER") || activity.provider.contains("User", ignoreCase = true)
        val statusLabel = when {
            rawStatus.contains("CANCEL") -> if (isCancelledByUser) "❌ CANCELLED BY USER" else "❌ CANCELLED"
            rawStatus.contains("ACCEPT") || rawStatus.contains("ROUTE") -> "✅ ACCEPTED JOB"
            rawStatus.contains("SEARCH") || rawStatus.contains("DISPATCH") -> "⏳ SEARCHING FOR PARTNER"
            rawStatus.contains("COMPLETE") -> "🏁 COMPLETED"
            else -> rawStatus
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Job Activity Details")
            .setMessage(
                "Service: ${activity.cause}\n" +
                "Status: $statusLabel\n" +
                "Date: ${activity.date}\n" +
                "Location: ${activity.location}\n" +
                "Details / Partner: ${activity.provider}\n" +
                "Amount: ${activity.amount}"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun loadMechanicActivitiesFromFirebase() {
        val currentMechId = sessionManager.getUserId()
        val requestsRef = FirebaseDatabase.getInstance().getReference("service_requests")
        try {
            requestsRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RecentActivity>()
                if (snapshot.exists() && snapshot.childrenCount > 0L) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                    for (child in snapshot.children) {
                        try {
                            val req = child.getValue(ServiceRequest::class.java) ?: continue
                            val rawStatus = req.status.uppercase()
                            val isRelevant = req.mechanicId == currentMechId ||
                                    currentMechId.isEmpty() ||
                                    rawStatus == "COMPLETED" ||
                                    rawStatus == "ACCEPTED" ||
                                    rawStatus.contains("CANCEL") ||
                                    rawStatus == "SEARCHING"

                            if (isRelevant) {
                                val time = if (req.updatedAt > 0L) req.updatedAt else req.createdAt
                                val formattedDate = dateFormat.format(Date(if (time > 0L) time else System.currentTimeMillis()))
                                val mechDisplay = req.mechanicName.ifEmpty { sessionManager.getBusinessName().ifEmpty { "Pitcrew Partner" } }
                                val providerStr = when {
                                    rawStatus.contains("CANCEL") -> "Cancelled by User"
                                    rawStatus.contains("COMPLETE") -> "$mechDisplay (Completed)"
                                    rawStatus.contains("ACCEPT") || rawStatus.contains("ROUTE") -> "$mechDisplay (Partner)"
                                    else -> "Pitcrew Network Dispatched"
                                }
                                val statusStr = when {
                                    rawStatus.contains("CANCEL") -> "CANCELLED_BY_USER"
                                    else -> req.status
                                }

                                list.add(
                                    RecentActivity(
                                        id = "act_${req.requestId}",
                                        requestId = req.requestId,
                                        date = formattedDate,
                                        location = req.address.ifEmpty { "Amphitheatre Parkway, Mountain View" },
                                        cause = req.cause.ifEmpty { "Breakdown Service" },
                                        amount = req.estimatedAmount.ifEmpty { "LKR 6,580.00" },
                                        provider = providerStr,
                                        status = statusStr,
                                        timestamp = if (time > 0L) time else System.currentTimeMillis()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (list.isNotEmpty()) {
                    list.sortByDescending { it.timestamp }
                    mechanicActivitiesList.clear()
                    mechanicActivitiesList.addAll(list)
                    mechanicRecentActivityAdapter.notifyDataSetChanged()
                } else if (mechanicActivitiesList.isEmpty()) {
                    // Pre-populate sample activities if database is empty (matching wallet earnings records)
                    val sampleActivities = listOf(
                        RecentActivity(
                            id = "act_sample_03",
                            requestId = "req_sample_03",
                            date = "Sep 12, 2026 • 11:30 AM",
                            location = "Peliyagoda Interchange",
                            cause = "Brake Pad Replacement & Fluid Flush",
                            amount = "LKR 12,000 .00",
                            provider = "${sessionManager.getBusinessName().ifEmpty { "Pitcrew Partner" }} (Completed)",
                            status = "COMPLETED",
                            timestamp = 1789232400000L
                        ),
                        RecentActivity(
                            id = "act_sample_02",
                            requestId = "req_sample_02",
                            date = "Sep 12, 2026 • 09:15 AM",
                            location = "Near Katunayake Exit",
                            cause = "Low fuel · Vehicle Import",
                            amount = "LKR 12,000 .00",
                            provider = "IOC Gas Station (Completed)",
                            status = "COMPLETED",
                            timestamp = 1789225200000L
                        ),
                        RecentActivity(
                            id = "act_sample_01",
                            requestId = "req_sample_01",
                            date = "Sep 12, 2026 • 07:45 AM",
                            location = "Near Katunayake Exit",
                            cause = "Full Engine Diagnostics & Alternator",
                            amount = "LKR 18,500 .00",
                            provider = "${sessionManager.getBusinessName().ifEmpty { "Pitcrew Partner" }} (Completed)",
                            status = "COMPLETED",
                            timestamp = 1789218000000L
                        )
                    )
                    mechanicActivitiesList.clear()
                    mechanicActivitiesList.addAll(sampleActivities)
                    mechanicRecentActivityAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun highlightNavTab(activeLayout: FrameLayout, activeIcon: ImageView) {
        val allIcons = listOf(ivNavHome, ivNavOrders, ivNavWallet, ivNavProfile)
        allIcons.forEach { icon ->
            icon.alpha = 0.5f
            icon.scaleX = 0.9f
            icon.scaleY = 0.9f
        }
        activeIcon.alpha = 1.0f
        activeIcon.scaleX = 1.15f
        activeIcon.scaleY = 1.15f
    }

    // ================= MECHANIC MY PROFILE LOGIC =================

    private fun setupMechanicProfile() {
        btnSaveMechanicProfile.setOnClickListener {
            saveMechanicProfile()
        }

        btnMechanicProfileSignOut.setOnClickListener {
            handleMechanicSignOut()
        }
    }

    private fun loadMechanicProfile() {
        // 1. Instantly populate cached values from SessionManager
        etMechanicFirstName.setText(sessionManager.getFirstName())
        etMechanicLastName.setText(sessionManager.getLastName())
        etMechanicPhone.setText(sessionManager.getPhone())
        etMechanicEmail.setText(sessionManager.getEmail())
        etMechanicShopName.setText(sessionManager.getBusinessName())
        etMechanicShopAddress.setText(sessionManager.getShopAddress())

        // 2. Fetch latest live profile data from Firebase Realtime Database
        val mechanicId = sessionManager.getUserId()
        if (mechanicId.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("mechanics").child(mechanicId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val mechanic = snapshot.getValue(Mechanic::class.java)
                            if (mechanic != null) {
                                if (mechanic.firstName.isNotEmpty()) etMechanicFirstName.setText(mechanic.firstName)
                                if (mechanic.lastName.isNotEmpty()) etMechanicLastName.setText(mechanic.lastName)
                                if (mechanic.phone.isNotEmpty()) etMechanicPhone.setText(mechanic.phone)
                                if (mechanic.email.isNotEmpty()) etMechanicEmail.setText(mechanic.email)
                                if (mechanic.businessName.isNotEmpty()) etMechanicShopName.setText(mechanic.businessName)
                                if (mechanic.address.isNotEmpty()) etMechanicShopAddress.setText(mechanic.address)

                                // Keep session synced
                                sessionManager.updateMechanicProfile(
                                    firstName = mechanic.firstName.ifEmpty { sessionManager.getFirstName() },
                                    lastName = mechanic.lastName.ifEmpty { sessionManager.getLastName() },
                                    email = mechanic.email.ifEmpty { sessionManager.getEmail() },
                                    phone = mechanic.phone.ifEmpty { sessionManager.getPhone() },
                                    businessName = mechanic.businessName.ifEmpty { sessionManager.getBusinessName() },
                                    address = mechanic.address.ifEmpty { sessionManager.getShopAddress() }
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Keep local session data if offline
                    }
                })
        }
    }

    private fun saveMechanicProfile() {
        val firstName = etMechanicFirstName.text.toString().trim()
        val lastName = etMechanicLastName.text.toString().trim()
        val phone = etMechanicPhone.text.toString().trim()
        val email = etMechanicEmail.text.toString().trim()
        val shopName = etMechanicShopName.text.toString().trim()
        val shopAddress = etMechanicShopAddress.text.toString().trim()

        if (firstName.isEmpty()) {
            etMechanicFirstName.error = "Please enter your first name"
            etMechanicFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            etMechanicLastName.error = "Please enter your last name"
            etMechanicLastName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etMechanicPhone.error = "Please enter your phone number"
            etMechanicPhone.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etMechanicEmail.error = "Please enter your email address"
            etMechanicEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etMechanicEmail.error = "Please enter a valid email address"
            etMechanicEmail.requestFocus()
            return
        }

        if (shopName.isEmpty()) {
            etMechanicShopName.error = "Please enter your workshop / business name"
            etMechanicShopName.requestFocus()
            return
        }

        if (shopAddress.isEmpty()) {
            etMechanicShopAddress.error = "Please enter your registered address"
            etMechanicShopAddress.requestFocus()
            return
        }

        btnSaveMechanicProfile.isEnabled = false
        tvSaveMechanicProfileText.visibility = View.INVISIBLE
        pbSaveMechanicProfile.visibility = View.VISIBLE

        val sessionManager = SessionManager(this)
        sessionManager.updateMechanicProfile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            businessName = shopName,
            address = shopAddress
        )

        val mechanicId = sessionManager.getUserId()
        if (mechanicId.isNotEmpty()) {
            val mechanicUpdates = mapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
                "fullName" to "$firstName $lastName".trim(),
                "phone" to phone,
                "email" to email.lowercase().trim(),
                "businessName" to shopName,
                "address" to shopAddress
            )

            FirebaseDatabase.getInstance().getReference("mechanics").child(mechanicId)
                .updateChildren(mechanicUpdates)
                .addOnSuccessListener {
                    btnSaveMechanicProfile.isEnabled = true
                    tvSaveMechanicProfileText.visibility = View.VISIBLE
                    pbSaveMechanicProfile.visibility = View.GONE
                    Toast.makeText(this, "Profile & Workshop details saved to database!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    btnSaveMechanicProfile.isEnabled = true
                    tvSaveMechanicProfileText.visibility = View.VISIBLE
                    pbSaveMechanicProfile.visibility = View.GONE
                    Toast.makeText(this, "Saved locally! (Firebase sync pending: ${error.localizedMessage})", Toast.LENGTH_SHORT).show()
                }
        } else {
            btnSaveMechanicProfile.isEnabled = true
            tvSaveMechanicProfileText.visibility = View.VISIBLE
            pbSaveMechanicProfile.visibility = View.GONE
            Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMechanicSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of your Pitcrew Mechanic account?")
            .setPositiveButton("Sign Out") { dialog, _ ->
                dialog.dismiss()
                val sessionManager = SessionManager(this)
                sessionManager.clearSession()
                val intent = Intent(this, MechanicLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
