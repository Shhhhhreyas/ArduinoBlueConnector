package com.iot.arduino.bluetooth.connector

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
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
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "com.bluetooth.switch"
    private val description = "Switches"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        Toast.makeText(appContext, "Connecting to ... ${device?.name} mac: ${device?.uuids?.getOrNull(0)} address: ${device?.address}", Toast.LENGTH_LONG).show()
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

    private fun createNotif(){

        val intent = Intent(this,ActionReceiver::class.java)
        val pendingIntentLight: PendingIntent = PendingIntent.getBroadcast(appContext,0,intent.putExtra("action","light"),PendingIntent.FLAG_MUTABLE)
        val pendingIntentFan: PendingIntent = PendingIntent.getBroadcast(appContext,1,intent.putExtra("action","fan"),PendingIntent.FLAG_MUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)
            notifBuilder = Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher_foreground))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .addAction(R.drawable.bulb_background,"Light",pendingIntentLight)
                .addAction(R.drawable.bulb_background,"Fan",pendingIntentFan)
        } else {
            notifBuilder = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher_foreground))
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(R.drawable.bulb_background,"Light",pendingIntentLight)
                .addAction(R.drawable.bulb_background,"Fan",pendingIntentFan)
        }
        notificationManager.notify(1234, notifBuilder.build())
    }
}