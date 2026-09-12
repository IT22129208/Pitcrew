package com.example.pitcrewa1

import android.app.Application
import com.example.pitcrewa1.data.Mechanic
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import java.io.File

class PitcrewApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Enable Firebase Realtime Database Disk Persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Initialize Global OSMDroid Configuration & Tile Cache
        try {
            val osmConfig = Configuration.getInstance()
            osmConfig.load(this, getSharedPreferences("pitcrew_osmdroid", MODE_PRIVATE))
            osmConfig.userAgentValue = "PitcrewRoadsideCustomerApp/2.0 (Android; support@pitcrewapp.io)"
            val basePath = File(cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            osmConfig.osmdroidBasePath = basePath
            osmConfig.osmdroidTileCache = tileCache
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Ensure all Firebase tables (mechanics, service_requests, activities, card_details) are initialized in console
        seedInitialFirebaseTables()
    }

    private fun seedInitialFirebaseTables() {
        try {
            val db = FirebaseDatabase.getInstance()

            // Check & seed sample mechanic if /mechanics is empty
            val mechanicsRef = db.getReference("mechanics")
            mechanicsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        val sampleMechanic = Mechanic(
                            mechanicId = "mech_demo_01",
                            firstName = "Kamal",
                            lastName = "Perera",
                            fullName = "Kamal Perera",
                            phone = "+94 77 987 6543",
                            businessName = "Pitcrew Express Auto Garage",
                            email = "mechanic@pitcrew.lk",
                            password = "password123",
                            latitude = 6.9833,
                            longitude = 79.8833,
                            address = "Katunayake Highway Exit, Negombo Rd",
                            isOnline = true,
                            isVerified = true,
                            rating = 4.9,
                            totalJobsCompleted = 42,
                            createdAt = System.currentTimeMillis()
                        )
                        mechanicsRef.child("mech_demo_01").setValue(sampleMechanic)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
