package com.mindorks.ridesharing.ui.maps

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.AnimationUtils
import com.mindorks.ridesharing.utils.MapUtils
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {


    companion object{
        private var TAG="MapsActivity"
        private var LOCATION_PERMISSION_REQUEST=999
        private var PICKUP_REQUEST=1
        private var DROP_REQUEST=2
    }

    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var movingCabMarker: Marker? = null
    private var blackPolyLine: Polyline? = null
    private var greyPolyLine: Polyline? = null
    private lateinit var mMap: GoogleMap
    private lateinit var presenter: MapsPresenter
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng?=null
    private var pickUpLatlng: LatLng?=null
    private var dropAtLatLng: LatLng?=null
    private var previousLatLngFromServer:LatLng?=null
    private var currentLatLngFromServer: LatLng?=null
    private val nearByCabListmarker= arrayListOf<Marker>()

    private fun enableLocationMap(){
        mMap.setPadding(0,ViewUtils.dpToPx(48.3f),0,0)
        mMap.isMyLocationEnabled=true
    }
    private fun moveCamera(latlng:LatLng?){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
    }
    private fun animateCamera(latlng: LatLng?){
        val cameraPosition = CameraPosition.Builder().target(latlng).zoom(15.5f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
    private fun AddCarmarker(latlng: LatLng):Marker{
        val bitmapDescriptor=BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(this))
        return mMap.addMarker(MarkerOptions().position(latlng).flat(true).icon(bitmapDescriptor))
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
                            setCurrentLocationAsPickUP()
                            enableLocationMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                            presenter.requestNearByCabs(currentLatLng!!)
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
        setUpClickListener()
    }

    private fun setUpClickListener() {
        pickUpTextView.setOnClickListener{
            launchlocationAutoComplete(PICKUP_REQUEST)
        }
        dropTextView.setOnClickListener {
            launchlocationAutoComplete(DROP_REQUEST)
        }
        requestCabButton.setOnClickListener{
            statusTextView.visibility=View.VISIBLE
            statusTextView.text=getString(R.string.requesting_your_cab)
            pickUpTextView.isEnabled=false
            dropTextView.isEnabled=false
            requestCabButton.isEnabled=false
            presenter.requestCab(pickUpLatlng!!,dropAtLatLng!!)
        }
        nextRideButton.setOnClickListener {
            reset()
        }
    }
    private fun reset(){
        statusTextView.visibility=View.GONE
        nextRideButton.visibility=View.GONE
        nearByCabListmarker.forEach { it.remove() }
        nearByCabListmarker.clear()
        currentLatLngFromServer=null
        previousLatLngFromServer=null
        if(currentLatLng!=null){
            moveCamera(currentLatLng)
            animateCamera(currentLatLng)
            setCurrentLocationAsPickUP()
            presenter.requestNearByCabs(currentLatLng!!)

        }
        else{
            pickUpTextView.text=""
        }
        pickUpTextView.isEnabled=true
        dropTextView.isEnabled=true
        dropTextView.text=""
        movingCabMarker?.remove()
        blackPolyLine?.remove()
        greyPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
        dropAtLatLng=null
        blackPolyLine=null
        greyPolyLine=null
        originMarker=null
        destinationMarker=null
        movingCabMarker=null
    }
    private fun CheckAndRequestCabButton(){
        if(pickUpLatlng!=null && dropAtLatLng!=null){
            requestCabButton.visibility= View.VISIBLE
            requestCabButton.isEnabled=true
        }
    }
    private fun launchlocationAutoComplete(requestCode: Int) {
        val field: List<Place.Field> = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, field).build(this)
        startActivityForResult(intent, requestCode)
    }
    private fun setCurrentLocationAsPickUP(){
        pickUpLatlng=currentLatLng
        pickUpTextView.text="Current Location"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== PICKUP_REQUEST || requestCode== DROP_REQUEST){
            when(resultCode){
                Activity.RESULT_OK ->{
                    val place= Autocomplete.getPlaceFromIntent(data!!)
                    when(requestCode){
                        PICKUP_REQUEST->{
                            pickUpTextView.text=place.name
                            pickUpLatlng=place.latLng
                            CheckAndRequestCabButton()
                        }
                        DROP_REQUEST->{
                            dropTextView.text=place.name
                            dropAtLatLng=place.latLng
                            CheckAndRequestCabButton()
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR->{
                    val status: Status=Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG, status.statusMessage)
                }
                Activity.RESULT_CANCELED->{

                }
            }
        }
    }

    override fun onDestroy() {
        presenter.onDetach()
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun showDirectionApiFailedError(error: String) {
        Toast.makeText(this,error,Toast.LENGTH_SHORT).show()
        reset()
    }

    override fun showRoutesNotAvailableError() {
        val error="Route not found"
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
    override fun ShowNearByCabs(LatLngList: List<LatLng>) {
        nearByCabListmarker.clear()
        for (latlng in LatLngList){
            val nearBycabMarker=AddCarmarker(latlng)
            nearByCabListmarker.add(nearBycabMarker)
        }
    }

    override fun informCanBooked() {
        nearByCabListmarker.forEach { it.remove() }
        nearByCabListmarker.clear()
        requestCabButton.visibility = View.GONE
        statusTextView.text = getString(R.string.your_cab_is_booked)

    }

    override fun showPath(latlnList: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latlnList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))
        val polyLineOptions = PolylineOptions()
        polyLineOptions.color(Color.GRAY)
        polyLineOptions.width(5f)
        polyLineOptions.addAll(latlnList)
        greyPolyLine = mMap.addPolyline(polyLineOptions)

        val blackpolyLineOptions = PolylineOptions()
        blackpolyLineOptions.color(Color.GRAY)
        blackpolyLineOptions.width(5f)
        blackPolyLine = mMap.addPolyline(blackpolyLineOptions)

        originMarker = addOriginDetinationMarkerAndGet(latlnList[0])
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addOriginDetinationMarkerAndGet(latlnList[latlnList.size - 1])
        destinationMarker?.setAnchor(0.5f,0.5f)
        val polyLineAnimator = AnimationUtils.polyLineAnimator()
        polyLineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (greyPolyLine?.points!!.size) * (percentValue / 100.0f).toInt()
            blackPolyLine?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polyLineAnimator.start()

    }

    override fun updateCabLocation(latLng: LatLng) {
        if(movingCabMarker==null){
            movingCabMarker=AddCarmarker(latLng)
        }
        if(previousLatLngFromServer==null){
            currentLatLngFromServer=latLng
            previousLatLngFromServer=currentLatLngFromServer
            movingCabMarker?.position=currentLatLngFromServer
            movingCabMarker?.setAnchor(0.5f,0.5f)
            animateCamera(currentLatLngFromServer)
        }
        else{
            previousLatLngFromServer=currentLatLngFromServer
            currentLatLngFromServer=latLng
            val valueAnimator=AnimationUtils.cabAnimator()
            valueAnimator.addUpdateListener {va->
                if(currentLatLngFromServer!=null && previousLatLngFromServer!=null){
                    val multiplier =va.animatedFraction
                    val nextLocation =LatLng(
                        multiplier*currentLatLngFromServer!!.latitude+(1-multiplier)*previousLatLngFromServer!!.latitude,
                        multiplier*currentLatLngFromServer!!.longitude+(1-multiplier)*previousLatLngFromServer!!.longitude
                    )
                    movingCabMarker?.position=nextLocation
                    var rotation=MapUtils.getRotation(previousLatLngFromServer!!,nextLocation)
                    if(!rotation.isNaN()){
                        movingCabMarker?.rotation=rotation
                    }
                    movingCabMarker?.setAnchor(0.5f,0.5f)
                    animateCamera(nextLocation)
                }

            }
            valueAnimator.start()
        }
    }

    override fun informCabArrived() {
       statusTextView.text=getString(R.string.your_cab_arrived)
        greyPolyLine?.remove()
        blackPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
    }

    override fun informCabIsArriving() {
        statusTextView.text=getString(R.string.your_cab_isArriving)
    }

    override fun informTripEnd() {
     statusTextView.text=getString(R.string.trip_finished)
        nextRideButton.visibility=View.VISIBLE
        greyPolyLine?.remove()
        blackPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
    }

    override fun informTripStart() {
      statusTextView.text=getString(R.string.trip_started)
        previousLatLngFromServer=null
    }
    private fun addOriginDetinationMarkerAndGet(latlng: LatLng): Marker {
        val bitmapDescriptor=BitmapDescriptorFactory.fromBitmap(MapUtils.getDestinationBitmap())
        return mMap.addMarker(MarkerOptions().position(latlng).flat(true).icon(bitmapDescriptor))
    }
}
