package com.iot.arduino.bluetooth.connector

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var  mBluetoothAdapter:BluetoothAdapter
    private lateinit var appContext:Context
    private lateinit var btSocket:BluetoothSocket
    private lateinit var myUUID: UUID
    private lateinit var lightButtonTurnOn:Button
    private lateinit var lightButtonTurnOff:Button
    private lateinit var fanButtonTurnOn:Button
    private lateinit var fanButtonTurnOff:Button
    private lateinit var connect:Button
    private lateinit var connection:TextView
    private lateinit var requestBluetooth:ActivityResultLauncher<Intent>
    private lateinit var requestMultiplePermissions:ActivityResultLauncher<Array<String>>
    private lateinit var notifBuilder:Notification.Builder
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private val channelId = "com.bluetooth.switch"
    private val description = "Switches"
    private var light:Boolean = true
    private var fan:Boolean = true

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(broadcastReceiver, IntentFilter("Action"))
        appContext = this.applicationContext
        myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        setContentView(R.layout.activity_main)
        requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(appContext,"Permission granted",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(appContext,"Permission denied",Toast.LENGTH_LONG).show()
            }
        }
        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("test006", "${it.key} = ${it.value}")
                }
            }
        lightButtonTurnOn = findViewById(R.id.lightTurnOn)
        lightButtonTurnOff = findViewById(R.id.lightTurnOff)
        fanButtonTurnOn = findViewById(R.id.fanTurnOn)
        fanButtonTurnOff = findViewById(R.id.fanTurnOff)
        connect = findViewById(R.id.connect)
        connection = findViewById(R.id.connectionStatus)
        connection.setText(R.string.notConnected)
        connect.setOnClickListener{
        connect()
        }
        lightButtonTurnOn.setOnClickListener{
           writeData("a")
        }
        lightButtonTurnOff.setOnClickListener{
            writeData("b")
        }
        fanButtonTurnOn.setOnClickListener{
            writeData("c")
        }
        fanButtonTurnOff.setOnClickListener{
            writeData("d")
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotif()
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("action").toString()) {
                "light" -> {
                    light = if(light) {
                        writeData("b")
                        false
                    } else{
                        writeData("a")
                        true
                    }
                }
                "fan" -> {
                    fan = if(fan) {
                        writeData("d")
                        false
                    } else{
                        writeData("c")
                        true
                    }
                }
                "connect" -> {
                    connect()
                }
            }
        }
    }

    private fun checkBt(): Int {
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        Log.d("1","1")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMultiplePermissions.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT))
            }
            else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }
            //return 0
        }
        if(!mBluetoothAdapter.enable()){
            Toast.makeText(appContext,"Bluetooth disabled!",Toast.LENGTH_LONG).show()
            return 0
        }
        return 1
    }

    private fun connect() {
        if(checkBt()==0) {
            return
        }
        val pairedDevices: Set<BluetoothDevice>? = mBluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
        }
        Log.d("Devices",pairedDevices.toString())
        val device = mBluetoothAdapter.getRemoteDevice("00:21:11:01:1A:DB") ?: return
        Toast.makeText(appContext, "Connecting to ... ${device.name} mac: ${device.uuids?.getOrNull(0)} address: ${device.address}", Toast.LENGTH_LONG).show()
        Log.d("", "Connecting to ... $device")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
           Toast.makeText(appContext,"Bluetooth connect permission not provided",Toast.LENGTH_LONG).show()

        }
        mBluetoothAdapter.cancelDiscovery()
        try {
            btSocket = device.createRfcommSocketToServiceRecord(myUUID)
            /* Here is the part the connection is made, by asking the device to create a RfcommSocket (Unsecure socket I guess), It map a port for us or something like that */
            btSocket.connect()
            Log.d("", "Connection made.")
            Toast.makeText(appContext, "Connection made.", Toast.LENGTH_SHORT).show()
            connection.setText(R.string.connected)
        } catch (e: IOException ) {
            Log.d("Failure",e.toString())
            try {
                btSocket.close()
            } catch (e2: IOException) {
                Log.d("", "Unable to end the connection")
                Toast.makeText(appContext , "Unable to end the connection", Toast.LENGTH_SHORT).show()
            }
            Log.d("", "Socket creation failed")
            Toast.makeText(appContext, "Socket creation failed: $e", Toast.LENGTH_SHORT).show()
        }

        //beginListenForData()
        /* this is a method used to read what the Arduino says for example when you write Serial.print("Hello world.") in your Arduino code */
    }

    private fun writeData(data: String) {
        if(!::btSocket.isInitialized) {
            Toast.makeText(appContext,"Device not connected",Toast.LENGTH_LONG).show()
            return
        }
        val outStream: OutputStream
        try {
            outStream = btSocket.outputStream
            val msgBuffer = data.toByteArray()
            try {
                outStream.write(msgBuffer)
            } catch (e: IOException) {
                //Log.d(FragmentActivity.TAG, "Bug while sending stuff", e)
            }
        } catch (e: IOException) {
            //Log.d(FragmentActivity.TAG, "Bug BEFORE Sending stuff", e)
        }



    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createNotif(){

        val intent = Intent(this,ActionReceiver::class.java)
        val pendingIntentLight: PendingIntent = PendingIntent.getBroadcast(appContext,0,intent.putExtra("action","light"),PendingIntent.FLAG_MUTABLE)
        val pendingIntentFan: PendingIntent = PendingIntent.getBroadcast(appContext,1,intent.putExtra("action","fan"),PendingIntent.FLAG_MUTABLE)
        val pendingIntentConnect: PendingIntent = PendingIntent.getBroadcast(appContext,2,intent.putExtra("action","connect"),PendingIntent.FLAG_MUTABLE)
        val lightAction:Notification.Action = Notification.Action(R.drawable.light,"Light",pendingIntentLight)
        val fanAction:Notification.Action = Notification.Action(R.drawable.fan,"Light",pendingIntentFan)
        val connectAction:Notification.Action = Notification.Action(R.drawable.connect,"Light",pendingIntentConnect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)
            notifBuilder = Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .addAction(lightAction)
                .addAction(fanAction)
                .addAction(connectAction)
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0,1,2))
        } else {
            notifBuilder = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher_foreground))
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(lightAction)
                .addAction(fanAction)
                .addAction(connectAction)
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0,1,2))
        }
        notificationManager.notify(1234, notifBuilder.build())
    }
}
