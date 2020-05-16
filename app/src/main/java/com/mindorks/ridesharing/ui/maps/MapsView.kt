package com.mindorks.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {
    fun ShowNearByCabs(LatLngList: List<LatLng>)
}