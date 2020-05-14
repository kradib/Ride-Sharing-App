package com.mindorks.ridesharing.ui.maps

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {


    companion object{
        private var TAG="MapsActivity"
        private var LOCATION_PERMISSION_REQUEST=999
    }
    private lateinit var mMap: GoogleMap
    private lateinit var presenter: MapsPresenter
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng?=null

    private fun enableLocationMap(){

    }
    private fun moveCamera(latlng:LatLng?){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
    }
    private fun animateCamera(latlng: LatLng?){
        val cameraPosition = CameraPosition.Builder().target(latlng).zoom(15.5f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun setUpLocationList(){
        fusedLocationProviderClient= FusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback=object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                if(currentLatLng==null){
                    for(location in p0.locations){
                        if(currentLatLng==null){
                            currentLatLng=LatLng(location.latitude,location.longitude)
                            enableLocationMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                        }
                    }
                }
            }
        }
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest,locationCallback,
            Looper.myLooper())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        presenter= MapsPresenter(NetworkService())
        presenter.onAttach(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onStart() {
        super.onStart()
        when{
            PermissionUtils.isAccessFineLocationGranted(this)->{
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        setUpLocationList()
                    }
                    else -> {
                        PermissionUtils.showGPSNOTEnable(this)
                    }
                }
            }
            else ->{
                PermissionUtils.reqestAccesssFindLocationPermission(this,
                    LOCATION_PERMISSION_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            LOCATION_PERMISSION_REQUEST->{
                if(grantResults.isNotEmpty()&& grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationList()
                        }
                        else -> {
                            PermissionUtils.showGPSNOTEnable(this)
                        }
                    }
                }
                else{
                    Toast.makeText(this,"Location Permission Not granted",Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    override fun onDestroy() {
        presenter.onDetach()
        super.onDestroy()
    }
}
