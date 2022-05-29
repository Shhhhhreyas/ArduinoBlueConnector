package com.iot.arduino.bluetooth.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        p0?.sendBroadcast(Intent("Action").putExtra("action",p1?.getStringExtra("action")))
    }
}