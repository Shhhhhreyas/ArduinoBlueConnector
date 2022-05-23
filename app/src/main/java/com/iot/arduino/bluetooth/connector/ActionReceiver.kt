package com.iot.arduino.bluetooth.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.getStringExtra("action")){
            "light" -> Log.d("Action", "light")
            "fan" -> Log.d("Action", "fan")
        }
    }
}