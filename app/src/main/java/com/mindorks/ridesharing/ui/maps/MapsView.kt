package com.mindorks.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {
    fun ShowNearByCabs(LatLngList: List<LatLng>)
    fun informCanBooked()
    fun showPath(latlnList:List<LatLng>)
    fun updateCabLocation(latLng: LatLng)
    fun informCabIsArriving()
    fun informCabArrived()
    fun informTripStart()
    fun informTripEnd()
    fun showRoutesNotAvailableError()
    fun showDirectionApiFailedError(error:String)
}