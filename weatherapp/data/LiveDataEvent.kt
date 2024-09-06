package com.example.weatherapp.data

data class LiveDataEvent<out T>(private val content: T){
    private var hasBeenHandle = false


    fun getContentIfNotHandled(): T? {
        return if(hasBeenHandle){
            null
        }else{
            hasBeenHandle = true
            content
        }
    }

    fun peekContent(): T = content
}
