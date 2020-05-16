package com.mindorks.ridesharing.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import com.mindorks.ridesharing.utils.Constants
import org.json.JSONObject

class MapsPresenter(private val networkService: NetworkService):WebSocketListener {

    companion object{
        private const val TAG="MapsPresenter"
    }

    private var view:MapsView?=null
    private lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView){
        this.view= view
        webSocket=networkService.createWebSocket(this)
        webSocket.connect()
    }
    fun onDetach(){
        webSocket.disconnect()
        view=null
    }
    fun requestNearByCabs(latlng:LatLng){
        val jsonObject= JSONObject()
        jsonObject.put(Constants.TYPE,Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT,latlng.latitude)
        jsonObject.put(Constants.LNG,latlng.longitude)
        webSocket.sendMessage(jsonObject.toString())

    }
    override fun onConnect() {
        Log.d(TAG,"onConnect")
    }

    override fun onMessage(data: String) {
        var jsonObject=JSONObject(data)
        when(jsonObject.get(Constants.TYPE)){
            Constants.NEAR_BY_CABS->{
                handleOnmessageNearByCabs(jsonObject)
            }
        }
    }

    private fun handleOnmessageNearByCabs(jsonObject: JSONObject) {
        var nearBycabLocations= arrayListOf<LatLng>()
        val jsonArray=jsonObject.getJSONArray(Constants.LOCATIONS)

        for (i in 0 until jsonArray.length()){
            var lat=(jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            var lng=(jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            var latlng= LatLng(lat,lng)
            nearBycabLocations.add(latlng)
        }
         view?.ShowNearByCabs(nearBycabLocations)

    }

    override fun onDisconnect() {
        Log.d(TAG,"onDisConnect")
    }

    override fun onError(error: String) {
        Log.d(TAG,"onError: $error")
    }
}