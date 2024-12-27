package com.example.cra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cra.ui.theme.CRATheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class MainActivity : ComponentActivity() {
    private lateinit var locationTracker: LocationTracker
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        firestore = FirebaseFirestore.getInstance()
        setContent {
            CRATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        val listPermissionsNeeded = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 1)
        } else {
            startTrackingLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            var allPermissionsGranted = true
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    when (permissions[i]) {
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            if (allPermissionsGranted) {
                startTrackingLocation()
            }
        }
    }

    private fun startTrackingLocation() {
        locationTracker = LocationTracker(this)
        locationTracker.startLocationUpdates()
        locationTracker.getLastLocation { location ->
            val locationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("locations").add(locationData)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "Location added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error adding location", e)
                }

            // Gửi vị trí đến server
            sendLocationToServer(location.latitude, location.longitude)
        }
    }

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:3000") // Thay thế bằng địa chỉ IP server của bạn
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.updateLocation(LocationData(latitude, longitude))

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Retrofit", "Location updated successfully")
                } else {
                    Log.d("Retrofit", "Failed to update location: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Retrofit", "Failed to update location", t)
            }
        })
    }

    interface ApiService {
        @POST("/update-location")
        fun updateLocation(@Body locationData: LocationData): Call<Void>
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double
    )

    override fun onDestroy() {
        super.onDestroy()
        if (this::locationTracker.isInitialized) {
            locationTracker.stopLocationUpdates()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CRATheme {
        Greeting("Android")
    }
}
