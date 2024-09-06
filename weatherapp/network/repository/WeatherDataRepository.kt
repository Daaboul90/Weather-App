package com.example.weatherapp.network.repository

import android.annotation.SuppressLint
import android.icu.util.UniversalTimeScale
import android.location.Geocoder
import com.example.weatherapp.data.CurrentLocation
import com.example.weatherapp.data.RemoteLocation
import com.example.weatherapp.data.RemoteWeatherData
import com.example.weatherapp.network.api.WeatherApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource

// This class is responsible for obtaining the user current geographical location. it use the FusedLocationProviderClient from Google Location Service API.
class WeatherDataRepository (private val weatherApi: WeatherApi){

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        fusedLocationProviderClient: FusedLocationProviderClient,
        // onSuccess a callback function that will be called if the location is successfully retrieved it take a current location object as parameter.
        onSuccess: (currentLocation: CurrentLocation) -> Unit,
        //onFailure A callBack function that will be called if the location retrieval fails.
        onFailure: () -> Unit
    ) {
        //Location Request
        //fusedLocationProviderClient.getCurrentLocation : This method request the current location from the FusedLocationProviderClient.
        fusedLocationProviderClient.getCurrentLocation(
            // This specifies that the location request should prioritize accuracy, which may consume more battery power.
            Priority.PRIORITY_HIGH_ACCURACY,
            // A cancellation token that allows the request to be cancelled if necessary.
            CancellationTokenSource().token
            // Handling the Response
            //This sets a listener that is triggered when the location request is successful.
        ).addOnSuccessListener { location->
            // If the location object is null, the onFailure callback is triggered. The (?:) operator is the Elvis operator, which is a shorthand for null check
            location ?: onFailure()
            onSuccess(
                CurrentLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )

        }.addOnFailureListener { onFailure() }

    }
  // This function takes a CurrentLocation object and a Geocoder object as inputs and attempts to update the CurrentLocation object with a human-readable address based on the latitude and longitude provided.
    @Suppress("DEPRECATION")
    fun updateAddressText(
        currentLocation: CurrentLocation,
        // used to convert geographic coordinates (latitude and longitude) into human-readable addresses.
        geocoder: Geocoder
    ): CurrentLocation{
        // Extracting Latitude and Longitude
        val latitude = currentLocation.latitude ?: return currentLocation
        val longitude = currentLocation.longitude ?: return currentLocation
      //This line uses the Geocoder to perform reverse geocoding, converting the latitude and longitude into a list of addresses. The 1 indicates that only one result (the best match) is desired.
      // let { addresses ->: The let function is a Kotlin scope function that allows you to perform operations on the result (addresses) within a lambda expression.
        return geocoder.getFromLocation(latitude, longitude, 1).let { addresses ->
            // Retrieves the first (and in this case, only) address from the list of results. The safe call operator (?.) ensures that if addresses is null, address will also be null.
            val address = addresses?.get(0)

            val addressText = StringBuilder()
            //Appends the locality (e.g., city or town) to the addressText, followed by a comma and a space. If address?.locality is null, nothing is appended.
            addressText.append(address?.locality).append(", ")
            //Appends the administrative area (e.g., state or region) in a similar fashion.
            addressText.append(address?.adminArea).append(", ")
            addressText.append(address?.countryName)
            // update the current location to a new location
            currentLocation.copy(
                //Sets the location field of the new CurrentLocation
                location = addressText.toString()
            )
        } ?: currentLocation

    }

    suspend fun searchLocation(query:String): List<RemoteLocation>? {
        val response = weatherApi.searchLocation(query = query)
        return if (response.isSuccessful) response.body() else null 
    }


    suspend fun getWeatherData(latitude: Double, longitude: Double): RemoteWeatherData?{
        val response = weatherApi.getWeatherData(query = "$latitude, $longitude")
        return if (response.isSuccessful) response .body() else null

    }

}