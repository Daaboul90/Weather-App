package com.example.weatherapp.fragments.home

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.text.BoringLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.R
import com.example.weatherapp.data.CurrentLocation
import com.example.weatherapp.data.WeatherData
import com.example.weatherapp.databinding.FragmentHomeBinding
import com.example.weatherapp.storage.SharedPreferencesManager
import com.google.android.gms.location.LocationServices
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment: Fragment() {

    companion object{
        const val REQUEST_KEY_MANUAL_LOCATION_SEARCH = "manualLocationSearch"
        const val KEY_LOCATION_TEXT = "locationText"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGTITUDE = "longitude"
    }
    //The underscore (_) prefix is a naming convention used to indicate that this variable is only accessible within this class, and it is usually the underlying representation of the binding property.
    private var _binding: FragmentHomeBinding?= null
    private val binding get() = requireNotNull(_binding)



    private val homeViewModel: HomeViewModel by viewModel()
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val geocoder by lazy { Geocoder(requireContext()) }



    private val weatherDataAdapter = WeatherDataAdapter(

        onLocationClicked = {
            showLocationOptions()
        }
    )


    private val sharedPreferencesManager: SharedPreferencesManager by inject()


    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
        if (isGranted){
            getCurrentLocation()
        }else{
            Toast.makeText(requireContext(),"Permission denied", Toast.LENGTH_SHORT).show()
        }

    }

    private var isInitialLocationSet:Boolean = false




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
        setWeatherDataAdapter()
        setCurrentLocation(currentLocation = sharedPreferencesManager.getCurrentLocation())
        setObservers()
        setListeners()
        if(!isInitialLocationSet){
            setCurrentLocation(currentLocation = sharedPreferencesManager.getCurrentLocation())
            isInitialLocationSet = true

        }

    }


    private fun setListeners(){
        binding.swiperRefreshLayout.setOnRefreshListener {
            setCurrentLocation(sharedPreferencesManager.getCurrentLocation())
        }
    }


    private fun setObservers(){
        with(homeViewModel){
            currentLocation.observe(viewLifecycleOwner){
                val currentLocationDataState = it.getContentIfNotHandled() ?: return@observe
                if (currentLocationDataState.isLoading){
                    showLoading()
                }
                currentLocationDataState.currentLocation?.let{ currentLocation ->
                    hideLoading()
                    sharedPreferencesManager.saveCurrentLocation(currentLocation)
                    setCurrentLocation(currentLocation)

                }
                currentLocationDataState.error?.let{ error ->
                    hideLoading()
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
            weatherData.observe(viewLifecycleOwner){
                val weatherDataState = it.getContentIfNotHandled() ?: return@observe
                binding.swiperRefreshLayout.isRefreshing = weatherDataState.isLoading
                weatherDataState.currentWeather?.let { currentWeather ->
                    weatherDataAdapter.setCurrentWeather(currentWeather)
                }
                weatherDataState.forecast?.let { forecasts ->
                    weatherDataAdapter.setForecastData(forecasts)

                }
                weatherDataState.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setWeatherDataAdapter(){
        binding.weatherDataRecyclerView.itemAnimator = null
        binding.weatherDataRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = weatherDataAdapter
        }


    }



    private fun setCurrentLocation(currentLocation: CurrentLocation? = null){

        weatherDataAdapter.setCurrentLocation(currentLocation ?: CurrentLocation())
        currentLocation?.let { getWeatherData(currentLocation = it ) }
    }



    private fun getCurrentLocation(){

        homeViewModel.getCurrentLocation(fusedLocationProviderClient, geocoder)
    }


    private fun isLocationPermissionGranted() : Boolean{
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION

        ) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestedLocationPermission(){
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun proccedWithCurrentLocation(){
        if (isLocationPermissionGranted()){
            getCurrentLocation()
        }else{
            requestedLocationPermission()
        }
    }



    private fun showLocationOptions(){
        val options = arrayOf("Current Location", "Search Manually")
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Chose Location Method")
            setItems(options){_, which ->
                when(which){
                    0-> proccedWithCurrentLocation()
                    1 -> startManualLocationSearch()
                }

            }
            show()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun showLoading(){
        with(binding){
            weatherDataRecyclerView.visibility = View.GONE
            swiperRefreshLayout.isEnabled = false
            swiperRefreshLayout.isRefreshing = true
        }
    }

    private fun hideLoading(){
        with(binding){
            weatherDataRecyclerView.visibility = View.VISIBLE
            swiperRefreshLayout.isEnabled = true
            swiperRefreshLayout.isRefreshing = false
        }
    }


    private fun startManualLocationSearch(){
        startListeningManualLocationSelection()
        findNavController().navigate(R.id.action_home_fragment_to_location_fragment)
    }

    private fun startListeningManualLocationSelection(){
        setFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH){_, bundle ->
            stopListeningManualLocationSelection()
            val currentLocation = CurrentLocation(
                location = bundle.getString(KEY_LOCATION_TEXT)?: "N/A",
                latitude = bundle.getDouble(KEY_LATITUDE),
                longitude = bundle.getDouble(KEY_LONGTITUDE)

            )
            sharedPreferencesManager.saveCurrentLocation(currentLocation)
            setCurrentLocation(currentLocation)
        }
    }

    private fun stopListeningManualLocationSelection(){
        clearFragmentResultListener(REQUEST_KEY_MANUAL_LOCATION_SEARCH)

    }


    private fun getWeatherData(currentLocation: CurrentLocation){
        if (currentLocation.latitude != null && currentLocation.longitude != null ){
            homeViewModel.getWeatherData(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude
            )
        }

    }

}