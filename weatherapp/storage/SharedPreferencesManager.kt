package com.example.weatherapp.storage

import android.content.Context
import androidx.core.content.edit
import com.example.weatherapp.data.CurrentLocation
import com.google.gson.Gson


// this class handles saving and retrieving the current location data in an Android application.


// This class is responsible for managing the saving and retrieving of the CurrentLocation object in the SharedPreferences.
class SharedPreferencesManager(context: Context, private val gson:Gson) {

    private companion object{
        const val PREF_NAME =  "WeatherAppPref"
        const val KEY_CURRENT_LOCATION = "currentLocation"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveCurrentLocation(currentLocation: CurrentLocation){
        //Converts the CurrentLocation object into a JSON string.
        val currentLocationJson = gson.toJson(currentLocation)
        // Opens an editor to make changes to the SharedPreferences.
        sharedPreferences.edit{
            putString(KEY_CURRENT_LOCATION, currentLocationJson)

        }
    }


    //retrieves the current location from SharedPreferences.
    fun getCurrentLocation(): CurrentLocation?{
        return sharedPreferences.getString(
            KEY_CURRENT_LOCATION,
            null
        )?.let{currentLocationJson ->
            gson.fromJson(currentLocationJson, CurrentLocation::class.java)


        }
    }
}