package com.example.cra

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ClientActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationTextView: TextView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        // Thiết lập user agent để tải bản đồ từ OSM servers
        Configuration.getInstance().userAgentValue = packageName

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        locationTextView = findViewById(R.id.locationTextView)

        database = FirebaseDatabase.getInstance().getReference("locations")

        val mapController = mapView.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(51.505, -0.09)
        mapController.setCenter(startPoint)

        val startMarker = Marker(mapView)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Start point"
        mapView.overlays.add(startMarker)

        // Lắng nghe dữ liệu vị trí thay đổi từ Firebase Realtime Database
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val locationData = data.getValue(LocationData::class.java)
                    locationData?.let {
                        val location = GeoPoint(it.latitude, it.longitude)
                        locationTextView.text = "Latitude: ${it.latitude}, Longitude: ${it.longitude}, Timestamp: ${it.timestamp}"
                        updateMap(location)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi
            }
        })
    }

    private fun updateMap(location: GeoPoint) {
        val mapController = mapView.controller
        mapController.setCenter(location)

        val marker = Marker(mapView)
        marker.position = location
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Current Location"
        mapView.overlays.clear()
        mapView.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume() // cần thiết cho compass, my location overlays
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // cần thiết cho compass, my location overlays
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach() // Dừng cập nhật bản đồ khi activity bị hủy
    }
}
