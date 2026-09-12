package com.example.pitcrewa1

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.NestedScrollView
import com.example.pitcrewa1.adapters.RecentActivityAdapter
import com.example.pitcrewa1.adapters.WalletCardAdapter
import com.example.pitcrewa1.data.CardDetail
import com.example.pitcrewa1.data.RecentActivity
import com.example.pitcrewa1.data.ServiceRequest
import com.example.pitcrewa1.data.User
import com.example.pitcrewa1.utils.CardStorageManager
import com.example.pitcrewa1.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), LocationListener {

    private enum class AppScreenState {
        GRID_SELECTION,
        OTHER_INPUT,
        SET_LOCATION_AND_PAY,
        SEARCHING_PARTNER,
        RECENT_ACTIVITIES,
        WALLET,
        PROFILE
    }

    private enum class PaymentMethod {
        CARD,
        CASH
    }

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private var userMarker: Marker? = null
    private var currentLocation: Location? = null

    private var currentState: AppScreenState = AppScreenState.GRID_SELECTION
    private var selectedCause: String = "Engine trouble"
    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.CARD

    // Map Center Drop Pin
    private lateinit var centerPinContainer: LinearLayout

    // Top Headers
    private lateinit var headerLayerDefault: ConstraintLayout
    private lateinit var headerLayerSetLocation: LinearLayout
    private lateinit var headerLayerSearchingPartner: LinearLayout
    private lateinit var tvSearchingSubtitle: TextView
    private lateinit var tvSearchingTitle: TextView
    private lateinit var tvLiveLocationAddress: TextView
    private lateinit var btnProfileAvatar: ImageView
    private lateinit var btnEmergencySiren: ImageView
    private lateinit var btnRecenterGps: ImageView
    private lateinit var bottomSheetGuideline: Guideline

    // View 1: 2x2 Grid
    private lateinit var layoutDefaultGrid: ConstraintLayout
    private lateinit var cardTirePunch: LinearLayout
    private lateinit var cardEngineTrouble: LinearLayout
    private lateinit var cardLowFuel: LinearLayout
    private lateinit var cardOther: LinearLayout

    // View 2: Expanded "Other" Issue Interface
    private lateinit var layoutOtherExpanded: NestedScrollView
    private lateinit var cardTirePunchExpanded: LinearLayout
    private lateinit var cardEngineTroubleExpanded: LinearLayout
    private lateinit var cardLowFuelExpanded: LinearLayout
    private lateinit var etIssueDescription: EditText
    private lateinit var btnGetHelpOther: TextView

    // View 3: "Pay Using" Bottom Panel
    private lateinit var layoutPayUsingPanel: LinearLayout
    private lateinit var layoutPayOptionCard: LinearLayout
    private lateinit var layoutPayOptionCash: LinearLayout
    private lateinit var ivRadioCard: ImageView
    private lateinit var ivRadioCash: ImageView
    private lateinit var btnFinalGetHelp: TextView

    // View 4: "Searching for a partner" Live Status Panel
    private lateinit var layoutSearchingPartnerPanel: LinearLayout
    private lateinit var tvSearchingCauseTitle: TextView
    private lateinit var tvSearchingPaymentMethod: TextView
    private lateinit var searchingStateContainer: LinearLayout
    private lateinit var pbSearchingRadar: ProgressBar
    private lateinit var tvSearchingStatusText: TextView
    private lateinit var layoutAssignedPartnerInfo: LinearLayout
    private lateinit var tvAssignedMechanicName: TextView
    private lateinit var tvAssignedMechanicEta: TextView
    private lateinit var tvAssignedMechanicPhone: TextView
    private lateinit var btnCancelServiceRequest: TextView

    // Active Service Request Tracking
    private var activeRequestId: String? = null
    private var activeActivityKey: String? = null
    private var activeRequestListener: ValueEventListener? = null
    private var activeCounterOfferDialog: BottomSheetDialog? = null
    private var mechanicMarker: Marker? = null

    // View 4: Recent Activities Screen Layer
    private lateinit var layoutRecentActivities: NestedScrollView
    private lateinit var recentActivitiesHeader: LinearLayout
    private lateinit var rvRecentActivities: RecyclerView
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private val recentActivitiesList = mutableListOf<RecentActivity>()

    // View 5: Wallet Screen Layer
    private lateinit var layoutWallet: NestedScrollView
    private lateinit var walletHeader: LinearLayout
    private lateinit var rvWalletCards: RecyclerView
    private lateinit var btnAddCard: View
    private lateinit var walletCardAdapter: WalletCardAdapter
    private val walletCardsList = mutableListOf<CardDetail>()
    private lateinit var cardStorageManager: CardStorageManager

    // View 6: Profile Screen Layer
    private lateinit var layoutProfile: NestedScrollView
    private lateinit var profileHeader: LinearLayout
    private lateinit var ivProfileAvatarLarge: ImageView
    private lateinit var etProfileFirstName: EditText
    private lateinit var etProfileLastName: EditText
    private lateinit var etProfilePhone: EditText
    private lateinit var etProfileEmail: EditText
    private lateinit var etProfileVehicleModel: EditText
    private lateinit var etProfileVehicleNumber: EditText
    private lateinit var etProfileVehicleType: EditText
    private lateinit var btnSaveProfile: View
    private lateinit var tvSaveProfileText: TextView
    private lateinit var pbSaveProfile: ProgressBar
    private lateinit var btnProfileSignOut: TextView

    // Bottom Navigation & Bottom Sheet Container
    private lateinit var bottomNavigationDock: LinearLayout
    private lateinit var bottomSheetPanel: View
    private lateinit var navHome: FrameLayout
    private lateinit var navOrders: FrameLayout
    private lateinit var navWallet: FrameLayout
    private lateinit var navProfile: FrameLayout
    private lateinit var ivNavHome: ImageView
    private lateinit var ivNavOrders: ImageView
    private lateinit var ivNavWallet: ImageView
    private lateinit var ivNavProfile: ImageView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            startRealTimeLocationUpdates()
        } else {
            tvLiveLocationAddress.text = "📍 Location Permission Needed"
            Toast.makeText(this, "Please grant location permission to view your live map location", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize OSMDroid Configuration with compliant User-Agent for unwatermarked free tiles
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("pitcrew_osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "PitcrewAutoCareCustomerApp/1.0 (Android; support@pitcrewapp.io)"

        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        initViews()
        setupRealMapView()
        setupEdgeToEdgeInsets()
        setupInteractions()
        setupBottomNavigation()
        setupBackPressHandler()

        // Check & request real-time location
        checkAndRequestLocationPermissions()
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        centerPinContainer = findViewById(R.id.centerPinContainer)

        headerLayerDefault = findViewById(R.id.headerLayerDefault)
        headerLayerSetLocation = findViewById(R.id.headerLayerSetLocation)
        headerLayerSearchingPartner = findViewById(R.id.headerLayerSearchingPartner)
        tvSearchingSubtitle = findViewById(R.id.tvSearchingSubtitle)
        tvSearchingTitle = findViewById(R.id.tvSearchingTitle)
        tvLiveLocationAddress = findViewById(R.id.tvLiveLocationAddress)
        btnRecenterGps = findViewById(R.id.btnRecenterGps)
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar)
        btnEmergencySiren = findViewById(R.id.btnEmergencySiren)
        bottomSheetGuideline = findViewById(R.id.bottomSheetGuideline)

        // View 1 (Grid)
        layoutDefaultGrid = findViewById(R.id.layoutDefaultGrid)
        cardTirePunch = findViewById(R.id.cardTirePunch)
        cardEngineTrouble = findViewById(R.id.cardEngineTrouble)
        cardLowFuel = findViewById(R.id.cardLowFuel)
        cardOther = findViewById(R.id.cardOther)

        // View 2 (Expanded Other)
        layoutOtherExpanded = findViewById(R.id.layoutOtherExpanded)
        cardTirePunchExpanded = findViewById(R.id.cardTirePunchExpanded)
        cardEngineTroubleExpanded = findViewById(R.id.cardEngineTroubleExpanded)
        cardLowFuelExpanded = findViewById(R.id.cardLowFuelExpanded)
        etIssueDescription = findViewById(R.id.etIssueDescription)
        btnGetHelpOther = findViewById(R.id.btnGetHelpOther)

        // View 3 (Pay Using)
        layoutPayUsingPanel = findViewById(R.id.layoutPayUsingPanel)
        layoutPayOptionCard = findViewById(R.id.layoutPayOptionCard)
        layoutPayOptionCash = findViewById(R.id.layoutPayOptionCash)
        ivRadioCard = findViewById(R.id.ivRadioCard)
        ivRadioCash = findViewById(R.id.ivRadioCash)
        btnFinalGetHelp = findViewById(R.id.btnFinalGetHelp)

        // View 4 (Searching For A Partner)
        layoutSearchingPartnerPanel = findViewById(R.id.layoutSearchingPartnerPanel)
        tvSearchingCauseTitle = findViewById(R.id.tvSearchingCauseTitle)
        tvSearchingPaymentMethod = findViewById(R.id.tvSearchingPaymentMethod)
        searchingStateContainer = findViewById(R.id.searchingStateContainer)
        pbSearchingRadar = findViewById(R.id.pbSearchingRadar)
        tvSearchingStatusText = findViewById(R.id.tvSearchingStatusText)
        layoutAssignedPartnerInfo = findViewById(R.id.layoutAssignedPartnerInfo)
        tvAssignedMechanicName = findViewById(R.id.tvAssignedMechanicName)
        tvAssignedMechanicEta = findViewById(R.id.tvAssignedMechanicEta)
        tvAssignedMechanicPhone = findViewById(R.id.tvAssignedMechanicPhone)
        btnCancelServiceRequest = findViewById(R.id.btnCancelServiceRequest)

        btnCancelServiceRequest.setOnClickListener {
            cancelActiveServiceRequest()
        }

        // View 4: Recent Activities
        layoutRecentActivities = findViewById(R.id.layoutRecentActivities)
        recentActivitiesHeader = findViewById(R.id.recentActivitiesHeader)
        rvRecentActivities = findViewById(R.id.rvRecentActivities)

        setupRecentActivitiesList()

        // View 5: Wallet
        cardStorageManager = CardStorageManager(this)
        layoutWallet = findViewById(R.id.layoutWallet)
        walletHeader = findViewById(R.id.walletHeader)
        rvWalletCards = findViewById(R.id.rvWalletCards)
        btnAddCard = findViewById(R.id.btnAddCard)

        setupWallet()

        // View 6: Profile
        layoutProfile = findViewById(R.id.layoutProfile)
        profileHeader = findViewById(R.id.profileHeader)
        ivProfileAvatarLarge = findViewById(R.id.ivProfileAvatarLarge)
        etProfileFirstName = findViewById(R.id.etProfileFirstName)
        etProfileLastName = findViewById(R.id.etProfileLastName)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        etProfileVehicleModel = findViewById(R.id.etProfileVehicleModel)
        etProfileVehicleNumber = findViewById(R.id.etProfileVehicleNumber)
        etProfileVehicleType = findViewById(R.id.etProfileVehicleType)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        tvSaveProfileText = findViewById(R.id.tvSaveProfileText)
        pbSaveProfile = findViewById(R.id.pbSaveProfile)
        btnProfileSignOut = findViewById(R.id.btnProfileSignOut)

        setupProfile()

        // Navigation & Bottom Sheet Container
        bottomNavigationDock = findViewById(R.id.bottomNavigationDock)
        bottomSheetPanel = findViewById(R.id.bottomSheetPanel)
        navHome = findViewById(R.id.navHome)
        navOrders = findViewById(R.id.navOrders)
        navWallet = findViewById(R.id.navWallet)
        navProfile = findViewById(R.id.navProfile)

        ivNavHome = findViewById(R.id.ivNavHome)
        ivNavOrders = findViewById(R.id.ivNavOrders)
        ivNavWallet = findViewById(R.id.ivNavWallet)
        ivNavProfile = findViewById(R.id.ivNavProfile)
    }

    private fun setupWallet() {
        rvWalletCards.layoutManager = LinearLayoutManager(this)

        // Load persisted cards from local disk storage immediately so cards are NEVER lost on restart
        val localCards = cardStorageManager.getCards()
        walletCardsList.clear()
        walletCardsList.addAll(localCards)

        walletCardAdapter = WalletCardAdapter(walletCardsList) { card ->
            showEditCardDialog(card)
        }
        rvWalletCards.adapter = walletCardAdapter

        btnAddCard.setOnClickListener {
            showAddCardDialog()
        }

        loadCardsFromFirebase()
    }

    private fun loadCardsFromFirebase() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId().ifEmpty { "default_user" }
        val cardsRef = FirebaseDatabase.getInstance().getReference("card_details").child(userId)

        try {
            cardsRef.keepSynced(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cardsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0L) {
                    val list = mutableListOf<CardDetail>()
                    for (child in snapshot.children) {
                        try {
                            child.getValue(CardDetail::class.java)?.let {
                                list.add(it)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (list.isNotEmpty()) {
                        walletCardsList.clear()
                        walletCardsList.addAll(list)
                        walletCardAdapter.notifyDataSetChanged()
                        cardStorageManager.saveCards(list)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep local disk-cached cards on network/permission issues
            }
        })
    }

    private fun showEditCardDialog(card: CardDetail) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_card, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val etCardNumber = dialogView.findViewById<EditText>(R.id.etEditCardNumber)
        val etCardHolder = dialogView.findViewById<EditText>(R.id.etEditCardHolder)
        val etCardExpiry = dialogView.findViewById<EditText>(R.id.etEditCardExpiry)
        val tvBadge = dialogView.findViewById<TextView>(R.id.tvEditCardTypeBadge)
        val btnUpdateCard = dialogView.findViewById<View>(R.id.btnDialogUpdateCard)
        val tvUpdateText = dialogView.findViewById<TextView>(R.id.tvDialogUpdateCardText)
        val pbUpdate = dialogView.findViewById<ProgressBar>(R.id.pbDialogUpdateCard)
        val btnDeleteCard = dialogView.findViewById<View>(R.id.btnDialogDeleteCard)
        val tvDeleteText = dialogView.findViewById<TextView>(R.id.tvDialogDeleteCardText)
        val pbDelete = dialogView.findViewById<ProgressBar>(R.id.pbDialogDeleteCard)

        // Pre-fill existing card data
        etCardNumber.setText(if (card.cardNumber.isNotEmpty()) card.cardNumber else card.maskedNumber)
        etCardHolder.setText(card.cardHolderName)
        etCardExpiry.setText(card.expiryDate)
        tvBadge.text = if (card.cardType.isNotEmpty()) card.cardType.uppercase() else "VISA"

        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId().ifEmpty { "default_user" }

        // Update Card
        btnUpdateCard.setOnClickListener {
            val rawNumber = etCardNumber.text.toString().trim()
            val holder = etCardHolder.text.toString().trim()
            val expiry = etCardExpiry.text.toString().trim()

            if (holder.isEmpty()) {
                etCardHolder.error = "Please enter cardholder name"
                etCardHolder.requestFocus()
                return@setOnClickListener
            }

            if (expiry.isEmpty()) {
                etCardExpiry.error = "Please enter expiry date"
                etCardExpiry.requestFocus()
                return@setOnClickListener
            }

            btnUpdateCard.isEnabled = false
            tvUpdateText.visibility = View.INVISIBLE
            pbUpdate.visibility = View.VISIBLE

            // Format masked card number if changed
            val digitsOnly = rawNumber.replace(" ", "")
            val masked = if (digitsOnly.length >= 4) {
                val prefix = if (digitsOnly.length >= 2) digitsOnly.substring(0, 2) else "10"
                val suffix = digitsOnly.substring(digitsOnly.length - 2)
                "${prefix}xx xxxx xxxx xx${suffix}"
            } else {
                card.maskedNumber
            }

            val cardType = if (digitsOnly.startsWith("4")) "VISA" else if (digitsOnly.startsWith("5")) "MASTERCARD" else card.cardType

            val updatedCard = card.copy(
                cardNumber = if (rawNumber.isNotEmpty()) rawNumber else card.cardNumber,
                maskedNumber = masked,
                cardHolderName = holder,
                expiryDate = expiry,
                cardType = cardType
            )

            // 1. Immediately persist to local disk storage
            cardStorageManager.addOrUpdateCard(updatedCard)
            val index = walletCardsList.indexOfFirst { it.cardId == card.cardId }
            if (index != -1) {
                walletCardsList[index] = updatedCard
                walletCardAdapter.notifyItemChanged(index)
            }
            dialog.dismiss()

            // 2. Persist to Firebase Realtime Database
            val cardRef = FirebaseDatabase.getInstance().getReference("card_details").child(userId).child(card.cardId)
            cardRef.setValue(updatedCard)
                .addOnSuccessListener {
                    Toast.makeText(this, "Card updated in Firebase & Wallet!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Card updated locally!", Toast.LENGTH_SHORT).show()
                }
        }

        // Delete Card
        btnDeleteCard.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Payment Card")
                .setMessage("Are you sure you want to remove this card (${card.maskedNumber}) from your Pitcrew wallet?")
                .setPositiveButton("Delete") { confirmDialog, _ ->
                    confirmDialog.dismiss()
                    btnDeleteCard.isEnabled = false
                    tvDeleteText.visibility = View.INVISIBLE
                    pbDelete.visibility = View.VISIBLE

                    // 1. Immediately remove from local disk storage and UI
                    cardStorageManager.deleteCard(card.cardId)
                    val index = walletCardsList.indexOfFirst { it.cardId == card.cardId }
                    if (index != -1) {
                        walletCardsList.removeAt(index)
                        walletCardAdapter.notifyItemRemoved(index)
                    }
                    dialog.dismiss()

                    // 2. Remove from Firebase Realtime Database
                    val cardRef = FirebaseDatabase.getInstance().getReference("card_details").child(userId).child(card.cardId)
                    cardRef.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Card deleted from Firebase & Wallet", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Card removed from wallet", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun showAddCardDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val etCardNumber = dialogView.findViewById<EditText>(R.id.etDialogCardNumber)
        val etCardHolder = dialogView.findViewById<EditText>(R.id.etDialogCardHolder)
        val etCardExpiry = dialogView.findViewById<EditText>(R.id.etDialogCardExpiry)
        val etCardCvv = dialogView.findViewById<EditText>(R.id.etDialogCardCvv)
        val btnSaveCard = dialogView.findViewById<View>(R.id.btnDialogSaveCard)
        val tvSaveText = dialogView.findViewById<TextView>(R.id.tvDialogSaveCardText)
        val pbSave = dialogView.findViewById<ProgressBar>(R.id.pbDialogSaveCard)

        // Pre-fill holder name from session if available
        val sessionManager = SessionManager(this)
        val userName = sessionManager.getFullName()
        if (userName.isNotEmpty()) {
            etCardHolder.setText(userName)
        }

        btnSaveCard.setOnClickListener {
            val rawNumber = etCardNumber.text.toString().trim()
            val holder = etCardHolder.text.toString().trim()
            val expiry = etCardExpiry.text.toString().trim()
            val cvv = etCardCvv.text.toString().trim()

            if (rawNumber.length < 12) {
                etCardNumber.error = "Please enter a valid card number (at least 12 digits)"
                etCardNumber.requestFocus()
                return@setOnClickListener
            }

            if (holder.isEmpty()) {
                etCardHolder.error = "Please enter cardholder name"
                etCardHolder.requestFocus()
                return@setOnClickListener
            }

            if (expiry.isEmpty()) {
                etCardExpiry.error = "Please enter expiry date (MM/YY)"
                etCardExpiry.requestFocus()
                return@setOnClickListener
            }

            if (cvv.length < 3) {
                etCardCvv.error = "Enter 3-digit CVV"
                etCardCvv.requestFocus()
                return@setOnClickListener
            }

            btnSaveCard.isEnabled = false
            tvSaveText.visibility = View.INVISIBLE
            pbSave.visibility = View.VISIBLE

            // Format masked card number: e.g. "10xx xxxx xxxx xx99"
            val digitsOnly = rawNumber.replace(" ", "")
            val prefix = if (digitsOnly.length >= 2) digitsOnly.substring(0, 2) else "10"
            val suffix = if (digitsOnly.length >= 2) digitsOnly.substring(digitsOnly.length - 2) else "99"
            val masked = "${prefix}xx xxxx xxxx xx${suffix}"

            val userId = sessionManager.getUserId().ifEmpty { "default_user" }
            val cardsRef = FirebaseDatabase.getInstance().getReference("card_details").child(userId)
            val cardKey = cardsRef.push().key ?: UUID.randomUUID().toString()

            val newCard = CardDetail(
                cardId = cardKey,
                userId = userId,
                cardNumber = rawNumber,
                maskedNumber = masked,
                cardHolderName = holder,
                expiryDate = expiry,
                cardType = if (digitsOnly.startsWith("4")) "VISA" else if (digitsOnly.startsWith("5")) "MASTERCARD" else "VISA",
                createdAt = System.currentTimeMillis()
            )

            // 1. Immediately persist locally so card is NEVER lost across app restarts
            cardStorageManager.addOrUpdateCard(newCard)
            walletCardsList.add(newCard)
            walletCardAdapter.notifyItemInserted(walletCardsList.size - 1)
            dialog.dismiss()

            // 2. Persist to Firebase Realtime Database
            cardsRef.child(cardKey).setValue(newCard)
                .addOnSuccessListener {
                    Toast.makeText(this, "Card saved to Firebase database & Wallet!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Card saved! (Firebase sync queued)", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun setupRecentActivitiesList() {
        rvRecentActivities.layoutManager = LinearLayoutManager(this)

        val sampleActivities = listOf(
            RecentActivity(
                id = "act_sample_1",
                requestId = "req_sample_1",
                date = "Aug 10, 2026 • 02:45 PM",
                location = "Near Katunayake Exit, Expressway",
                cause = "Low Fuel",
                amount = "LKR 6,580.00",
                provider = "IOC Seeduwa Gas Station",
                status = "COMPLETED",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            ),
            RecentActivity(
                id = "act_sample_2",
                requestId = "req_sample_2",
                date = "Aug 05, 2026 • 11:20 AM",
                location = "Baseline Road, Colombo 09",
                cause = "Tire Punch",
                amount = "LKR 2,500.00",
                provider = "City Tyres Mobile Care",
                status = "ACCEPTED",
                timestamp = System.currentTimeMillis() - 86400000L * 7
            ),
            RecentActivity(
                id = "act_sample_3",
                requestId = "req_sample_3",
                date = "Jul 29, 2026 • 08:15 PM",
                location = "Galle Road, Mount Lavinia",
                cause = "Engine trouble",
                amount = "LKR 0.00",
                provider = "Cancelled by User",
                status = "CANCELLED_BY_USER",
                timestamp = System.currentTimeMillis() - 86400000L * 14
            ),
            RecentActivity(
                id = "act_sample_4",
                requestId = "req_sample_4",
                date = "Jul 15, 2026 • 04:30 PM",
                location = "Kandy Road, Kadawatha",
                cause = "Battery Jumpstart",
                amount = "LKR 4,200.00",
                provider = "AutoVolt Mobile Technicians",
                status = "COMPLETED",
                timestamp = System.currentTimeMillis() - 86400000L * 28
            )
        )
        recentActivitiesList.clear()
        recentActivitiesList.addAll(sampleActivities)

        recentActivityAdapter = RecentActivityAdapter(recentActivitiesList) { activity ->
            showActivityDetailDialog(activity)
        }
        rvRecentActivities.adapter = recentActivityAdapter

        loadActivitiesFromFirebase()
    }

    private fun showActivityDetailDialog(activity: RecentActivity) {
        val rawStatus = activity.status.uppercase()
        val isCancelledByUser = rawStatus.contains("USER") || activity.provider.contains("User", ignoreCase = true)
        val statusLabel = when {
            rawStatus.contains("CANCEL") -> if (isCancelledByUser) "❌ CANCELLED BY USER" else "❌ CANCELLED"
            rawStatus.contains("ACCEPT") || rawStatus.contains("ROUTE") -> "✅ ACCEPTED BY MECHANIC"
            rawStatus.contains("SEARCH") || rawStatus.contains("DISPATCH") -> "⏳ SEARCHING FOR PARTNER"
            rawStatus.contains("COMPLETE") -> "🏁 COMPLETED"
            else -> rawStatus
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Activity Details")
            .setMessage(
                "Service: ${activity.cause}\n" +
                "Status: $statusLabel\n" +
                "Date: ${activity.date}\n" +
                "Location: ${activity.location}\n" +
                "Provider: ${activity.provider}\n" +
                "Amount: ${activity.amount}"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun loadActivitiesFromFirebase() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty()) {
            val activitiesRef = FirebaseDatabase.getInstance().getReference("activities").child(userId)
            try {
                activitiesRef.keepSynced(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            activitiesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RecentActivity>()
                    if (snapshot.exists() && snapshot.childrenCount > 0L) {
                        for (child in snapshot.children) {
                            try {
                                child.getValue(RecentActivity::class.java)?.let {
                                    list.add(it)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        list.sortByDescending { it.timestamp }
                        recentActivitiesList.clear()
                        recentActivitiesList.addAll(list)
                        recentActivityAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Keep sample/local data if offline
                }
            })

            // Real-time synchronization with service_requests:
            // Ensures any status updates (e.g. COMPLETED or ACCEPTED by mechanic) immediately reconcile
            val requestsRef = FirebaseDatabase.getInstance().getReference("service_requests")
            requestsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    for (child in snapshot.children) {
                        val req = child.getValue(ServiceRequest::class.java) ?: continue
                        if (req.userId == userId) {
                            val actKey = "act_${req.requestId}"
                            val mechName = req.mechanicName.ifEmpty { "Pitcrew Partner" }
                            val targetStatus = when (req.status) {
                                "COMPLETED" -> "COMPLETED"
                                "ACCEPTED", "EN_ROUTE", "IN_PROGRESS" -> "ACCEPTED"
                                "CANCELLED" -> "CANCELLED_BY_USER"
                                else -> req.status
                            }
                            val targetProvider = when (targetStatus) {
                                "COMPLETED" -> "$mechName (Completed)"
                                "ACCEPTED" -> "$mechName (Partner)"
                                "CANCELLED_BY_USER" -> "Cancelled by User"
                                else -> "Pitcrew Network Dispatched"
                            }

                            // Update in-memory list if it exists
                            val idx = recentActivitiesList.indexOfFirst { it.id == actKey || it.requestId == req.requestId }
                            if (idx != -1) {
                                val current = recentActivitiesList[idx]
                                if (current.status != targetStatus || current.provider != targetProvider) {
                                    recentActivitiesList[idx] = current.copy(
                                        status = targetStatus,
                                        provider = targetProvider,
                                        amount = req.estimatedAmount.ifEmpty { current.amount }
                                    )
                                    recentActivityAdapter.notifyItemChanged(idx)
                                }
                            }

                            // If mechanic completed or accepted, ensure /activities node in Firebase has the true status
                            if (targetStatus == "COMPLETED" || targetStatus == "ACCEPTED" || targetStatus == "CANCELLED_BY_USER") {
                                val updates = mapOf<String, Any>(
                                    "status" to targetStatus,
                                    "provider" to targetProvider,
                                    "amount" to req.estimatedAmount.ifEmpty { "LKR 6,580.00" }
                                )
                                FirebaseDatabase.getInstance().getReference("activities")
                                    .child(userId)
                                    .child(actKey)
                                    .updateChildren(updates)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun setupRealMapView() {
        // Use ESRI World Street Map for crystal-clear, high-speed, 100% watermark-free, unblocked global road tiles
        val esriWorldStreetMap = object : OnlineTileSourceBase(
            "EsriWorldStreetMap",
            0,
            19,
            256,
            "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}$zoom/$y/$x"
            }
        }

        mapView.setTileSource(esriWorldStreetMap)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val mapController = mapView.controller
        mapController.setZoom(17.5)

        val startPoint = GeoPoint(6.9833, 79.8833)
        mapController.setCenter(startPoint)

        userMarker = Marker(mapView).apply {
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_user_location_pin)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            position = startPoint
            title = "My Live Location"
        }
        mapView.overlays.add(userMarker)
    }

    private fun setupEdgeToEdgeInsets() {
        val mainRoot = findViewById<View>(R.id.mainRoot)
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            headerLayerDefault.setPadding(
                headerLayerDefault.paddingLeft,
                systemBars.top + 8,
                headerLayerDefault.paddingRight,
                headerLayerDefault.paddingBottom
            )
            headerLayerSetLocation.setPadding(
                headerLayerSetLocation.paddingLeft,
                systemBars.top + 28,
                headerLayerSetLocation.paddingRight,
                headerLayerSetLocation.paddingBottom
            )
            headerLayerSearchingPartner.setPadding(
                headerLayerSearchingPartner.paddingLeft,
                systemBars.top + 28,
                headerLayerSearchingPartner.paddingRight,
                headerLayerSearchingPartner.paddingBottom
            )
            recentActivitiesHeader.setPadding(
                recentActivitiesHeader.paddingLeft,
                systemBars.top + 28,
                recentActivitiesHeader.paddingRight,
                recentActivitiesHeader.paddingBottom
            )
            walletHeader.setPadding(
                walletHeader.paddingLeft,
                systemBars.top + 28,
                walletHeader.paddingRight,
                walletHeader.paddingBottom
            )
            profileHeader.setPadding(
                profileHeader.paddingLeft,
                systemBars.top + 28,
                profileHeader.paddingRight,
                profileHeader.paddingBottom
            )

            // Dynamic bottom margin for floating bottom navigation bar
            val bottomNavParams = bottomNavigationDock.layoutParams as? ViewGroup.MarginLayoutParams
            val defaultMarginBottom = (14 * resources.displayMetrics.density).toInt()
            bottomNavParams?.bottomMargin = systemBars.bottom + defaultMarginBottom
            bottomNavigationDock.layoutParams = bottomNavParams

            // Handle IME (soft keyboard) height and visibility smoothly
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val keyboardHeight = if (isImeVisible) (ime.bottom - systemBars.bottom).coerceAtLeast(0) else 0

            if (keyboardHeight > 0) {
                bottomNavigationDock.visibility = View.GONE
                if (layoutOtherExpanded.visibility == View.VISIBLE || layoutPayUsingPanel.visibility == View.VISIBLE) {
                    bottomSheetPanel.translationY = -keyboardHeight.toFloat()
                }
            } else {
                bottomNavigationDock.visibility = View.VISIBLE
                bottomSheetPanel.translationY = 0f
            }

            insets
        }
    }

    private fun setupInteractions() {
        // Recenter GPS Button
        btnRecenterGps.setOnClickListener {
            animateGpsRecenter()
            currentLocation?.let { loc ->
                val userPoint = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.animateTo(userPoint, 18.0, 800L)
                Toast.makeText(this, "Centered to live position", Toast.LENGTH_SHORT).show()
            } ?: run {
                checkAndRequestLocationPermissions()
                Toast.makeText(this, "Acquiring live GPS fix...", Toast.LENGTH_SHORT).show()
            }
        }

        // Top-Left Profile Avatar
        btnProfileAvatar.setOnClickListener {
            highlightNavTab(navProfile, ivNavProfile)
            showProfileInterface()
        }

        // Top-Right Emergency SOS Siren
        btnEmergencySiren.setOnClickListener {
            dispatchEmergencyRequestImmediately()
        }

        // 2x2 Grid Cards Selection
        cardTirePunch.setOnClickListener {
            selectCauseAndProceed("Tire Punch")
        }

        cardEngineTrouble.setOnClickListener {
            selectCauseAndProceed("Engine trouble")
        }

        cardLowFuel.setOnClickListener {
            selectCauseAndProceed("Low Fuel")
        }

        cardOther.setOnClickListener {
            showOtherExpandedInterface()
        }

        // Expanded Horizontal Cards Selection
        cardTirePunchExpanded.setOnClickListener {
            selectCauseAndProceed("Tire Punch")
        }

        cardEngineTroubleExpanded.setOnClickListener {
            selectCauseAndProceed("Engine trouble")
        }

        cardLowFuelExpanded.setOnClickListener {
            selectCauseAndProceed("Low Fuel")
        }

        btnGetHelpOther.setOnClickListener {
            val customText = etIssueDescription.text.toString().trim()
            selectedCause = if (customText.isNotEmpty()) customText else "Custom Auto Issue"
            showSetLocationAndPayInterface()
        }

        // Payment Option Radios
        layoutPayOptionCard.setOnClickListener {
            selectPaymentMethod(PaymentMethod.CARD)
        }

        layoutPayOptionCash.setOnClickListener {
            selectPaymentMethod(PaymentMethod.CASH)
        }

        // Final "Get Help" Button
        btnFinalGetHelp.setOnClickListener {
            handleFinalDispatch()
        }
    }

    private fun selectCauseAndProceed(cause: String) {
        selectedCause = cause
        showSetLocationAndPayInterface()
    }

    private fun selectPaymentMethod(method: PaymentMethod) {
        selectedPaymentMethod = method
        if (method == PaymentMethod.CARD) {
            ivRadioCard.setImageResource(R.drawable.ic_radio_checked)
            ivRadioCash.setImageResource(R.drawable.ic_radio_unchecked)
        } else {
            ivRadioCard.setImageResource(R.drawable.ic_radio_unchecked)
            ivRadioCash.setImageResource(R.drawable.ic_radio_checked)
        }
    }

    private fun showSetLocationAndPayInterface() {
        currentState = AppScreenState.SET_LOCATION_AND_PAY

        // Switch Headers
        headerLayerDefault.visibility = View.GONE
        headerLayerSetLocation.visibility = View.VISIBLE
        headerLayerSearchingPartner.visibility = View.GONE

        // Switch Panels
        layoutRecentActivities.visibility = View.GONE
        layoutWallet.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.VISIBLE

        // Show Center Pin Drop on Map
        centerPinContainer.visibility = View.VISIBLE

        // Animate Guideline to 56% for perfect map-to-sheet balance
        animateGuideline(bottomSheetGuideline.layoutParams.let { (it as ConstraintLayout.LayoutParams).guidePercent }, 0.56f)

        // Center map on user location
        currentLocation?.let { loc ->
            mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    private fun showOtherExpandedInterface() {
        currentState = AppScreenState.OTHER_INPUT

        headerLayerDefault.visibility = View.VISIBLE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.GONE

        layoutRecentActivities.visibility = View.GONE
        layoutWallet.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        layoutOtherExpanded.visibility = View.VISIBLE

        centerPinContainer.visibility = View.GONE

        animateGuideline(bottomSheetGuideline.layoutParams.let { (it as ConstraintLayout.LayoutParams).guidePercent }, 0.12f)
        btnRecenterGps.visibility = View.GONE
        etIssueDescription.requestFocus()
    }

    private fun showDefaultGridInterface() {
        currentState = AppScreenState.GRID_SELECTION

        headerLayerDefault.visibility = View.VISIBLE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.GONE

        layoutRecentActivities.visibility = View.GONE
        layoutWallet.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        layoutDefaultGrid.visibility = View.VISIBLE

        centerPinContainer.visibility = View.GONE
        btnRecenterGps.visibility = View.VISIBLE

        animateGuideline(bottomSheetGuideline.layoutParams.let { (it as ConstraintLayout.LayoutParams).guidePercent }, 0.46f)
    }

    private fun showSearchingPartnerInterface(request: ServiceRequest) {
        currentState = AppScreenState.SEARCHING_PARTNER

        // Switch Headers
        headerLayerDefault.visibility = View.GONE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.VISIBLE

        // Switch Panels
        layoutRecentActivities.visibility = View.GONE
        layoutWallet.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.VISIBLE

        // Map elements
        centerPinContainer.visibility = View.GONE
        btnRecenterGps.visibility = View.VISIBLE

        // Bind request info
        tvSearchingCauseTitle.text = if (request.cause.isNotEmpty()) request.cause else selectedCause
        tvSearchingPaymentMethod.text = "Payment Method : ${request.paymentCardMasked.ifEmpty { if (request.paymentMethod == "CARD") "Card Payment" else "Cash to Technician on Site" }}"

        // Reset to searching state initially
        searchingStateContainer.visibility = View.VISIBLE
        layoutAssignedPartnerInfo.visibility = View.GONE
        tvSearchingStatusText.text = "Searching ...."
        tvSearchingTitle.text = "Searching for a partner"
        tvSearchingSubtitle.text = "Help is on its way..."

        // Adjust guideline for comfortable view of map and status card
        animateGuideline(bottomSheetGuideline.layoutParams.let { (it as ConstraintLayout.LayoutParams).guidePercent }, 0.58f)

        // Center map on user location
        currentLocation?.let { loc ->
            mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    private fun showRecentActivitiesInterface() {
        currentState = AppScreenState.RECENT_ACTIVITIES

        headerLayerDefault.visibility = View.GONE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.GONE

        layoutWallet.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        centerPinContainer.visibility = View.GONE
        btnRecenterGps.visibility = View.GONE

        layoutRecentActivities.visibility = View.VISIBLE
        loadActivitiesFromFirebase()
    }

    private fun showWalletInterface() {
        currentState = AppScreenState.WALLET

        headerLayerDefault.visibility = View.GONE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.GONE

        layoutRecentActivities.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        centerPinContainer.visibility = View.GONE
        btnRecenterGps.visibility = View.GONE

        layoutWallet.visibility = View.VISIBLE
    }

    private fun showProfileInterface() {
        currentState = AppScreenState.PROFILE

        headerLayerDefault.visibility = View.GONE
        headerLayerSetLocation.visibility = View.GONE
        headerLayerSearchingPartner.visibility = View.GONE

        layoutRecentActivities.visibility = View.GONE
        layoutWallet.visibility = View.GONE
        layoutDefaultGrid.visibility = View.GONE
        layoutOtherExpanded.visibility = View.GONE
        layoutPayUsingPanel.visibility = View.GONE
        layoutSearchingPartnerPanel.visibility = View.GONE
        centerPinContainer.visibility = View.GONE
        btnRecenterGps.visibility = View.GONE

        layoutProfile.visibility = View.VISIBLE
        loadUserProfile()
    }

    private fun animateGuideline(fromPercent: Float, toPercent: Float) {
        ValueAnimator.ofFloat(fromPercent, toPercent).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val percent = animator.animatedValue as Float
                bottomSheetGuideline.setGuidelinePercent(percent)
            }
            start()
        }
    }

    private fun handleFinalDispatch() {
        val sessionManager = SessionManager(this)
        val uid = sessionManager.getUserId().ifEmpty { "user_${UUID.randomUUID().toString().substring(0, 8)}" }
        val locText = tvLiveLocationAddress.text.toString().removePrefix("📍 ")
        val addressStr = if (locText.isNotEmpty() && !locText.contains("Finding")) locText else "Pacific Palisades, Colombo"

        // Determine payment info
        val cardMasked = if (selectedPaymentMethod == PaymentMethod.CARD) {
            val firstCard = walletCardsList.firstOrNull()
            firstCard?.maskedNumber ?: "xxxx xxxx xxxx x332"
        } else {
            "Cash to Technician on Site"
        }

        val lat = currentLocation?.latitude ?: 6.9833
        val lng = currentLocation?.longitude ?: 79.8833
        val newReqId = "req_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 5)}"
        val newActId = "act_${newReqId}"
        activeRequestId = newReqId
        activeActivityKey = newActId

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())

        val serviceReq = ServiceRequest(
            requestId = newReqId,
            userId = uid,
            customerName = sessionManager.getFullName().ifEmpty { "Customer" },
            customerPhone = sessionManager.getPhone().ifEmpty { "+94 77 123 4567" },
            customerEmail = sessionManager.getEmail(),
            vehicleModel = sessionManager.getVehicleModel().ifEmpty { "2023 BMW M4 Competition" },
            vehicleNumber = sessionManager.getVehicleNumber().ifEmpty { "WP CAB-1234" },
            vehicleType = sessionManager.getVehicleType().ifEmpty { "Coupe" },
            cause = selectedCause,
            description = etIssueDescription.text.toString().trim(),
            paymentMethod = if (selectedPaymentMethod == PaymentMethod.CARD) "CARD" else "CASH",
            paymentCardMasked = cardMasked,
            latitude = lat,
            longitude = lng,
            address = addressStr,
            status = "SEARCHING",
            estimatedAmount = "LKR 6,580.00",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Write to shared /service_requests/{requestId} database table
        val reqRef = FirebaseDatabase.getInstance().getReference("service_requests").child(newReqId)
        reqRef.setValue(serviceReq)
            .addOnSuccessListener {
                Toast.makeText(this, "Request broadcasted! Searching for partner...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Works offline via local disk sync
            }

        // 2. Also log to user's order activities list with SEARCHING status
        val newActivity = RecentActivity(
            id = newActId,
            requestId = newReqId,
            date = formattedDate,
            location = addressStr,
            cause = selectedCause,
            amount = "LKR 6,580.00",
            provider = "Pitcrew Network Dispatched",
            status = "SEARCHING",
            timestamp = System.currentTimeMillis()
        )
        recentActivitiesList.add(0, newActivity)
        recentActivityAdapter.notifyItemInserted(0)
        if (uid.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("activities").child(uid).child(newActId).setValue(newActivity)
        }

        // 3. Switch UI to "Searching for a partner" state
        showSearchingPartnerInterface(serviceReq)

        // 4. Attach real-time listener to the shared request node
        listenToActiveServiceRequest(newReqId)
    }

    private fun listenToActiveServiceRequest(requestId: String) {
        // Clean up any old listener
        activeRequestListener?.let { listener ->
            activeRequestId?.let { id ->
                FirebaseDatabase.getInstance().getReference("service_requests").child(id).removeEventListener(listener)
            }
        }

        val sessionManager = SessionManager(this)
        val uid = sessionManager.getUserId()
        val reqRef = FirebaseDatabase.getInstance().getReference("service_requests").child(requestId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val req = snapshot.getValue(ServiceRequest::class.java) ?: return

                when (req.status) {
                    "OFFER_PENDING_APPROVAL" -> {
                        // Vibrate alert for incoming counter offer
                        try {
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(500)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        searchingStateContainer.visibility = View.VISIBLE
                        layoutAssignedPartnerInfo.visibility = View.GONE
                        tvSearchingTitle.text = "⚡ Price Offer Changed"
                        tvSearchingSubtitle.text = "${req.mechanicName.ifEmpty { "Mechanic" }} offered ${req.estimatedAmount}"
                        tvSearchingStatusText.text = "Offer Received"

                        showOfferPriceChangedDialog(req)
                    }
                    "ACCEPTED", "EN_ROUTE", "IN_PROGRESS" -> {
                        activeCounterOfferDialog?.let { if (it.isShowing) it.dismiss() }
                        activeCounterOfferDialog = null

                        // A mechanic / merchant accepted the request!
                        searchingStateContainer.visibility = View.GONE
                        layoutAssignedPartnerInfo.visibility = View.VISIBLE
                        tvSearchingTitle.text = "Partner On The Way!"
                        tvSearchingSubtitle.text = "Mechanic is heading to your position"

                        val mechName = req.mechanicName.ifEmpty { "Verified Pitcrew Mechanic" }
                        val offerAmount = req.estimatedAmount.ifEmpty { "LKR 6,580.00" }
                        tvAssignedMechanicName.text = mechName
                        tvAssignedMechanicEta.text = if (req.etaMinutes > 0) "⚡ Arriving in ~${req.etaMinutes} mins" else "⚡ Arriving shortly (~8 mins)"
                        tvAssignedMechanicPhone.text = "📞 ${req.mechanicPhone.ifEmpty { "+94 77 987 6543" }}"

                        // Update activity in user's activities node to ACCEPTED
                        val actId = activeActivityKey ?: "act_${req.requestId}"
                        if (uid.isNotEmpty() && actId.isNotEmpty()) {
                            val updates = mapOf<String, Any>(
                                "status" to "ACCEPTED",
                                "provider" to "$mechName (Partner)",
                                "amount" to offerAmount,
                                "cause" to req.cause
                            )
                            FirebaseDatabase.getInstance().getReference("activities").child(uid).child(actId).updateChildren(updates)
                        }

                        // Immediate zero-delay UI update in local list
                        val localIdx = recentActivitiesList.indexOfFirst { it.id == actId || it.requestId == req.requestId }
                        if (localIdx != -1) {
                            val old = recentActivitiesList[localIdx]
                            recentActivitiesList[localIdx] = old.copy(
                                status = "ACCEPTED",
                                provider = "$mechName (Partner)",
                                amount = offerAmount,
                                cause = if (req.cause.isNotEmpty()) req.cause else old.cause
                            )
                            recentActivityAdapter.notifyItemChanged(localIdx)
                        }

                        // Place / update mechanic pin on map
                        if (req.mechanicLat != 0.0 && req.mechanicLng != 0.0) {
                            val mechPoint = GeoPoint(req.mechanicLat, req.mechanicLng)
                            if (mechanicMarker == null) {
                                mechanicMarker = Marker(mapView).apply {
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    title = req.mechanicName.ifEmpty { "Mechanic Location" }
                                }
                                mapView.overlays.add(mechanicMarker)
                            }
                            mechanicMarker?.position = mechPoint
                            mapView.invalidate()
                        }
                    }
                    "COMPLETED" -> {
                        activeCounterOfferDialog?.let { if (it.isShowing) it.dismiss() }
                        activeCounterOfferDialog = null

                        val actId = activeActivityKey ?: "act_${req.requestId}"
                        val mechName = req.mechanicName.ifEmpty { "Pitcrew Partner" }
                        val finalAmount = req.estimatedAmount.ifEmpty { "LKR 6,580.00" }

                        if (uid.isNotEmpty() && actId.isNotEmpty()) {
                            val updates = mapOf<String, Any>(
                                "status" to "COMPLETED",
                                "provider" to "$mechName (Completed)",
                                "amount" to finalAmount
                            )
                            FirebaseDatabase.getInstance().getReference("activities").child(uid).child(actId).updateChildren(updates)
                        }

                        // Immediate zero-delay UI update in local list
                        val localIdx = recentActivitiesList.indexOfFirst { it.id == actId || it.requestId == req.requestId }
                        if (localIdx != -1) {
                            val old = recentActivitiesList[localIdx]
                            recentActivitiesList[localIdx] = old.copy(
                                status = "COMPLETED",
                                provider = "$mechName (Completed)",
                                amount = finalAmount
                            )
                            recentActivityAdapter.notifyItemChanged(localIdx)
                        }

                        Toast.makeText(this@MainActivity, "Service completed! Thank you for choosing Pitcrew.", Toast.LENGTH_LONG).show()
                        cleanupActiveServiceRequest()
                        showDefaultGridInterface()
                    }
                    "CANCELLED" -> {
                        activeCounterOfferDialog?.let { if (it.isShowing) it.dismiss() }
                        activeCounterOfferDialog = null

                        val actId = activeActivityKey ?: "act_${req.requestId}"
                        if (uid.isNotEmpty() && actId.isNotEmpty()) {
                            val updates = mapOf<String, Any>(
                                "status" to "CANCELLED_BY_USER",
                                "provider" to "Cancelled by User",
                                "amount" to "LKR 0.00"
                            )
                            FirebaseDatabase.getInstance().getReference("activities").child(uid).child(actId).updateChildren(updates)
                        }
                        val localIdx = recentActivitiesList.indexOfFirst { it.id == actId || it.requestId == req.requestId }
                        if (localIdx != -1) {
                            val old = recentActivitiesList[localIdx]
                            recentActivitiesList[localIdx] = old.copy(
                                status = "CANCELLED_BY_USER",
                                provider = "Cancelled by User",
                                amount = "LKR 0.00"
                            )
                            recentActivityAdapter.notifyItemChanged(localIdx)
                        }
                        cleanupActiveServiceRequest()
                        showDefaultGridInterface()
                    }
                    else -> {
                        // Still SEARCHING
                        activeCounterOfferDialog?.let { if (it.isShowing) it.dismiss() }
                        activeCounterOfferDialog = null

                        searchingStateContainer.visibility = View.VISIBLE
                        layoutAssignedPartnerInfo.visibility = View.GONE
                        tvSearchingStatusText.text = "Searching ...."
                        tvSearchingTitle.text = "Searching for a partner"
                        tvSearchingSubtitle.text = "Help is on its way..."
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancelled/network issues gracefully
            }
        }
        activeRequestListener = listener
        reqRef.addValueEventListener(listener)
    }

    private fun showOfferPriceChangedDialog(request: ServiceRequest) {
        if (isFinishing || isDestroyed) return
        if (activeCounterOfferDialog?.isShowing == true) return

        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_offer_price_changed, null)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)

        val tvMechanicName = dialogView.findViewById<TextView>(R.id.tvDialogMechanicName)
        val tvMechanicEta = dialogView.findViewById<TextView>(R.id.tvDialogMechanicEta)
        val tvIssueCause = dialogView.findViewById<TextView>(R.id.tvDialogIssueCause)
        val tvOriginalPrice = dialogView.findViewById<TextView>(R.id.tvDialogOriginalPrice)
        val tvNewPrice = dialogView.findViewById<TextView>(R.id.tvDialogNewPrice)
        val btnAccept = dialogView.findViewById<FrameLayout>(R.id.btnAcceptCounterOffer)
        val tvAcceptText = dialogView.findViewById<TextView>(R.id.tvAcceptOfferButtonText)
        val pbAccept = dialogView.findViewById<ProgressBar>(R.id.pbAcceptOffer)
        val btnSearchAnother = dialogView.findViewById<FrameLayout>(R.id.btnSearchAnotherMerchant)

        val mechName = request.mechanicName.ifEmpty { "Pitcrew Verified Partner" }
        tvMechanicName.text = mechName
        tvMechanicEta.text = if (request.etaMinutes > 0) "⚡ ~${request.etaMinutes} mins away" else "⚡ ~6 mins away"
        tvIssueCause.text = "Issue: ${if (request.cause.isNotEmpty()) request.cause else "Auto Breakdown Assistance"}"
        tvOriginalPrice.text = request.originalAmount.ifEmpty { "LKR 6,580.00" }
        tvNewPrice.text = request.estimatedAmount.ifEmpty { "LKR 8,500.00" }
        tvAcceptText.text = "Accept Offer (${request.estimatedAmount})"

        btnAccept.setOnClickListener {
            tvAcceptText.visibility = View.INVISIBLE
            pbAccept.visibility = View.VISIBLE
            btnAccept.isEnabled = false
            btnSearchAnother.isEnabled = false

            val updates = mapOf<String, Any>(
                "status" to "ACCEPTED",
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseDatabase.getInstance().getReference("service_requests")
                .child(request.requestId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    val sessionManager = SessionManager(this)
                    val uid = sessionManager.getUserId()
                    val actId = activeActivityKey ?: "act_${request.requestId}"
                    if (uid.isNotEmpty()) {
                        val actUpdates = mapOf<String, Any>(
                            "status" to "ACCEPTED",
                            "provider" to "$mechName (Partner)",
                            "amount" to request.estimatedAmount,
                            "cause" to request.cause
                        )
                        FirebaseDatabase.getInstance().getReference("activities").child(uid).child(actId).updateChildren(actUpdates)
                    }
                    dialog.dismiss()
                    Toast.makeText(this, "Price accepted! Mechanic is on the way.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    tvAcceptText.visibility = View.VISIBLE
                    pbAccept.visibility = View.GONE
                    btnAccept.isEnabled = true
                    btnSearchAnother.isEnabled = true
                    Toast.makeText(this, "Accept failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnSearchAnother.setOnClickListener {
            btnAccept.isEnabled = false
            btnSearchAnother.isEnabled = false

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
                    dialog.dismiss()
                    Toast.makeText(this, "Offer declined. Searching for another merchant...", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    dialog.dismiss()
                }
        }

        activeCounterOfferDialog = dialog
        dialog.show()
    }

    private fun cancelActiveServiceRequest() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Request")
            .setMessage("Are you sure you want to cancel this assistance request?")
            .setPositiveButton("Yes, Cancel") { dialog, _ ->
                dialog.dismiss()
                val sessionManager = SessionManager(this)
                val uid = sessionManager.getUserId()
                val reqId = activeRequestId
                val actId = activeActivityKey ?: (if (reqId != null) "act_$reqId" else null)

                if (reqId != null) {
                    FirebaseDatabase.getInstance().getReference("service_requests").child(reqId).child("status").setValue("CANCELLED")
                }

                if (uid.isNotEmpty() && actId != null) {
                    val updates = mapOf<String, Any>(
                        "status" to "CANCELLED_BY_USER",
                        "provider" to "Cancelled by User",
                        "amount" to "LKR 0.00"
                    )
                    FirebaseDatabase.getInstance().getReference("activities").child(uid).child(actId).updateChildren(updates)
                }

                // Immediate zero-delay UI update in local list
                val localIdx = recentActivitiesList.indexOfFirst { it.id == actId || it.requestId == reqId }
                if (localIdx != -1) {
                    val old = recentActivitiesList[localIdx]
                    recentActivitiesList[localIdx] = old.copy(
                        status = "CANCELLED_BY_USER",
                        provider = "Cancelled by User",
                        amount = "LKR 0.00"
                    )
                    recentActivityAdapter.notifyItemChanged(localIdx)
                }

                Toast.makeText(this, "Request cancelled by user", Toast.LENGTH_SHORT).show()
                cleanupActiveServiceRequest()
                showDefaultGridInterface()
            }
            .setNegativeButton("Keep Waiting") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun cleanupActiveServiceRequest() {
        activeCounterOfferDialog?.let { if (it.isShowing) it.dismiss() }
        activeCounterOfferDialog = null

        activeRequestListener?.let { listener ->
            activeRequestId?.let { id ->
                FirebaseDatabase.getInstance().getReference("service_requests").child(id).removeEventListener(listener)
            }
        }
        activeRequestListener = null
        activeRequestId = null

        mechanicMarker?.let {
            mapView.overlays.remove(it)
            mechanicMarker = null
            mapView.invalidate()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentState == AppScreenState.SEARCHING_PARTNER) {
                    cancelActiveServiceRequest()
                } else if (currentState != AppScreenState.GRID_SELECTION) {
                    highlightNavTab(navHome, ivNavHome)
                    showDefaultGridInterface()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        highlightNavTab(navHome, ivNavHome)

        navHome.setOnClickListener {
            highlightNavTab(navHome, ivNavHome)
            if (currentState == AppScreenState.SEARCHING_PARTNER) {
                currentLocation?.let { loc ->
                    mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                }
            } else if (currentState != AppScreenState.GRID_SELECTION) {
                showDefaultGridInterface()
                currentLocation?.let { loc ->
                    mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        }

        navOrders.setOnClickListener {
            highlightNavTab(navOrders, ivNavOrders)
            showRecentActivitiesInterface()
        }

        navWallet.setOnClickListener {
            highlightNavTab(navWallet, ivNavWallet)
            showWalletInterface()
        }

        navProfile.setOnClickListener {
            highlightNavTab(navProfile, ivNavProfile)
            showProfileInterface()
        }
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

    private fun animateGpsRecenter() {
        val rotation = ObjectAnimator.ofFloat(btnRecenterGps, View.ROTATION, 0f, 360f).apply {
            duration = 600
        }
        rotation.start()
    }

    // ================= REAL-TIME LOCATION ENGINE =================

    private fun checkAndRequestLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            startRealTimeLocationUpdates()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealTimeLocationUpdates() {
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("📍 Enable Location Services")
                    .setMessage("Please enable GPS in settings so Pitcrew technicians can locate you on the map.")
                    .setPositiveButton("Settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("Later", null)
                    .show()
                return
            }

            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = lastGps ?: lastNetwork
            bestLast?.let { onLocationChanged(it) }

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    this
                )
            }

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    1f,
                    this
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        val userPoint = GeoPoint(location.latitude, location.longitude)

        // Move map camera to user position
        mapView.controller.animateTo(userPoint)
        userMarker?.position = userPoint
        mapView.invalidate()

        // Reverse geocode location address
        updateLocationUi(location.latitude, location.longitude)
    }

    override fun onProviderEnabled(provider: String) {
        startRealTimeLocationUpdates()
    }

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun updateLocationUi(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses: List<Address> ->
                    runOnUiThread {
                        val address = addresses.firstOrNull()
                        val text = if (address != null) {
                            val thoroughfare = address.thoroughfare ?: address.subLocality ?: address.locality ?: "Current Location"
                            val locality = address.locality ?: address.subAdminArea ?: ""
                            "📍 $thoroughfare${if (locality.isNotEmpty()) ", $locality" else ""}"
                        } else {
                            String.format(Locale.US, "📍 %.4f, %.4f", latitude, longitude)
                        }
                        tvLiveLocationAddress.text = text
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                val text = if (address != null) {
                    val thoroughfare = address.thoroughfare ?: address.subLocality ?: address.locality ?: "Current Location"
                    val locality = address.locality ?: address.subAdminArea ?: ""
                    "📍 $thoroughfare${if (locality.isNotEmpty()) ", $locality" else ""}"
                } else {
                    String.format(Locale.US, "📍 %.4f, %.4f", latitude, longitude)
                }
                tvLiveLocationAddress.text = text
            }
        } catch (e: Exception) {
            tvLiveLocationAddress.text = String.format(Locale.US, "📍 %.4f, %.4f", latitude, longitude)
        }
    }

    // ================= EMERGENCY SOS DISPATCH LOGIC =================

    private fun dispatchEmergencyRequestImmediately() {
        // Double-pulse haptic feedback for emergency trigger
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sessionManager = SessionManager(this)
        val uid = sessionManager.getUserId().ifEmpty { "user_${UUID.randomUUID().toString().substring(0, 8)}" }
        val locText = tvLiveLocationAddress.text.toString().removePrefix("📍 ")
        val addressStr = if (locText.isNotEmpty() && !locText.contains("Finding")) locText else "Live GPS Location"

        val lat = currentLocation?.latitude ?: 6.9833
        val lng = currentLocation?.longitude ?: 79.8833
        val newReqId = "req_emergency_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 5)}"
        val newActId = "act_${newReqId}"
        activeRequestId = newReqId
        activeActivityKey = newActId
        selectedCause = "🚨 Emergency SOS Breakdown"

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())

        val cardMasked = if (selectedPaymentMethod == PaymentMethod.CARD) {
            val firstCard = walletCardsList.firstOrNull()
            firstCard?.maskedNumber ?: "xxxx xxxx xxxx x332"
        } else {
            "Cash to Technician on Site (Emergency SOS)"
        }

        val emergencyRequest = ServiceRequest(
            requestId = newReqId,
            userId = uid,
            customerName = sessionManager.getFullName().ifEmpty { "Emergency Customer" },
            customerPhone = sessionManager.getPhone().ifEmpty { "+94 77 123 4567" },
            customerEmail = sessionManager.getEmail(),
            vehicleModel = sessionManager.getVehicleModel().ifEmpty { "2023 BMW M4 Competition" },
            vehicleNumber = sessionManager.getVehicleNumber().ifEmpty { "WP CAB-1234" },
            vehicleType = sessionManager.getVehicleType().ifEmpty { "Coupe" },
            cause = "🚨 Emergency SOS Breakdown",
            description = "URGENT EMERGENCY: Customer pressed SOS Siren. Immediate roadside assistance requested.",
            paymentMethod = if (selectedPaymentMethod == PaymentMethod.CARD) "CARD" else "CASH",
            paymentCardMasked = cardMasked,
            latitude = lat,
            longitude = lng,
            address = addressStr,
            status = "SEARCHING",
            estimatedAmount = "LKR 6,580.00",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Broadcast directly to /service_requests/{newReqId} in Firebase Realtime Database
        val reqRef = FirebaseDatabase.getInstance().getReference("service_requests").child(newReqId)
        reqRef.setValue(emergencyRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "🚨 Emergency SOS broadcasted to all nearby merchants & mechanics!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "🚨 Emergency broadcast queued (Offline mode active)", Toast.LENGTH_LONG).show()
            }

        // 2. Add to customer's order activity history with SEARCHING status
        val newActivity = RecentActivity(
            id = newActId,
            requestId = newReqId,
            date = formattedDate,
            location = addressStr,
            cause = "🚨 Emergency SOS Breakdown",
            amount = "LKR 6,580.00",
            provider = "🚨 Priority Emergency Dispatch",
            status = "SEARCHING",
            timestamp = System.currentTimeMillis()
        )
        recentActivitiesList.add(0, newActivity)
        recentActivityAdapter.notifyItemInserted(0)
        if (uid.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("activities").child(uid).child(newActId).setValue(newActivity)
        }

        // 3. Switch dashboard UI to Searching Partner state with Emergency messaging
        showSearchingPartnerInterface(emergencyRequest)
        tvSearchingTitle.text = "🚨 Emergency SOS Dispatched!"
        tvSearchingSubtitle.text = "Alerting all nearby mechanics with priority..."

        // 4. Attach real-time listener to track mechanic response & acceptance
        listenToActiveServiceRequest(newReqId)
    }

    // ================= MY PROFILE & VEHICLE DETAILS LOGIC =================

    private fun setupProfile() {
        btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        btnProfileSignOut.setOnClickListener {
            handleSignOut()
        }
    }

    private fun loadUserProfile() {
        val sessionManager = SessionManager(this)

        // 1. Instantly populate cached values from SessionManager
        etProfileFirstName.setText(sessionManager.getFirstName())
        etProfileLastName.setText(sessionManager.getLastName())
        etProfilePhone.setText(sessionManager.getPhone())
        etProfileEmail.setText(sessionManager.getEmail())
        etProfileVehicleModel.setText(sessionManager.getVehicleModel())
        etProfileVehicleNumber.setText(sessionManager.getVehicleNumber())
        etProfileVehicleType.setText(sessionManager.getVehicleType())

        // 2. Fetch latest live profile data from Firebase Realtime Database
        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null) {
                                if (user.firstName.isNotEmpty()) etProfileFirstName.setText(user.firstName)
                                if (user.lastName.isNotEmpty()) etProfileLastName.setText(user.lastName)
                                if (user.phoneNumber.isNotEmpty()) etProfilePhone.setText(user.phoneNumber)
                                if (user.email.isNotEmpty()) etProfileEmail.setText(user.email)
                                if (user.vehicleModel.isNotEmpty()) etProfileVehicleModel.setText(user.vehicleModel)
                                if (user.vehicleNumber.isNotEmpty()) etProfileVehicleNumber.setText(user.vehicleNumber)
                                if (user.vehicleType.isNotEmpty()) etProfileVehicleType.setText(user.vehicleType)

                                // Keep session synced
                                sessionManager.updateProfile(
                                    firstName = user.firstName.ifEmpty { sessionManager.getFirstName() },
                                    lastName = user.lastName.ifEmpty { sessionManager.getLastName() },
                                    email = user.email.ifEmpty { sessionManager.getEmail() },
                                    phone = user.phoneNumber.ifEmpty { sessionManager.getPhone() },
                                    vehicleModel = user.vehicleModel.ifEmpty { sessionManager.getVehicleModel() },
                                    vehicleNumber = user.vehicleNumber.ifEmpty { sessionManager.getVehicleNumber() },
                                    vehicleType = user.vehicleType.ifEmpty { sessionManager.getVehicleType() }
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

    private fun saveUserProfile() {
        val firstName = etProfileFirstName.text.toString().trim()
        val lastName = etProfileLastName.text.toString().trim()
        val phone = etProfilePhone.text.toString().trim()
        val email = etProfileEmail.text.toString().trim()
        val vehicleModel = etProfileVehicleModel.text.toString().trim()
        val vehicleNumber = etProfileVehicleNumber.text.toString().trim()
        val vehicleType = etProfileVehicleType.text.toString().trim()

        if (firstName.isEmpty()) {
            etProfileFirstName.error = "Please enter your first name"
            etProfileFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            etProfileLastName.error = "Please enter your last name"
            etProfileLastName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etProfilePhone.error = "Please enter your phone number"
            etProfilePhone.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etProfileEmail.error = "Please enter your email address"
            etProfileEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etProfileEmail.error = "Please enter a valid email address"
            etProfileEmail.requestFocus()
            return
        }

        btnSaveProfile.isEnabled = false
        tvSaveProfileText.visibility = View.INVISIBLE
        pbSaveProfile.visibility = View.VISIBLE

        val sessionManager = SessionManager(this)
        sessionManager.updateProfile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            vehicleType = vehicleType
        )

        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty()) {
            val userUpdates = mapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
                "phoneNumber" to phone,
                "email" to email.lowercase().trim(),
                "vehicleModel" to vehicleModel,
                "vehicleNumber" to vehicleNumber,
                "vehicleType" to vehicleType
            )

            FirebaseDatabase.getInstance().getReference("users").child(userId)
                .updateChildren(userUpdates)
                .addOnSuccessListener {
                    btnSaveProfile.isEnabled = true
                    tvSaveProfileText.visibility = View.VISIBLE
                    pbSaveProfile.visibility = View.GONE
                    Toast.makeText(this, "Profile & Vehicle details saved to database!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    btnSaveProfile.isEnabled = true
                    tvSaveProfileText.visibility = View.VISIBLE
                    pbSaveProfile.visibility = View.GONE
                    Toast.makeText(this, "Saved locally! (Firebase sync pending: ${error.localizedMessage})", Toast.LENGTH_SHORT).show()
                }
        } else {
            btnSaveProfile.isEnabled = true
            tvSaveProfileText.visibility = View.VISIBLE
            pbSaveProfile.visibility = View.GONE
            Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of your Pitcrew account?")
            .setPositiveButton("Sign Out") { dialog, _ ->
                dialog.dismiss()
                val sessionManager = SessionManager(this)
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOrdersDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📋 Service History & Requests")
            .setMessage("• Today: Live Standby\n• Sept 2: Synthetic Oil Change & Filter (Completed)\n• Aug 18: Tire Rotation & Pressure Check (Completed)")
            .setPositiveButton("Book New", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showWalletDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("💳 Pitcrew Wallet & Payments")
            .setMessage("Default Payment: Apple Pay / Visa •••• 4242\nPitcrew Balance: $120.00 Credits\nDiscount: 15% VIP Auto Care Member")
            .setPositiveButton("Add Funds", null)
            .setNegativeButton("Close", null)
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}