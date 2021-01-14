package com.himanshu.controller.activity

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.himanshu.controller.R
import java.io.IOException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnForward: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnBackward: Button
    private lateinit var btnShowAvailableDevices: Button

    private lateinit var txtSpeed: TextView
    private lateinit var txtMovement: TextView
    private lateinit var txtDistance: TextView
    private lateinit var txtObstacleString: TextView
    private lateinit var seekBarSpeed: SeekBar
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView
    private lateinit var rlDistance: RelativeLayout
    private lateinit var progressLayout: RelativeLayout

    private lateinit var btDevices: ArrayList<BluetoothDevice>
    private lateinit var addressList: ArrayList<String>
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var bluetoothDevices: ArrayList<String>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private var inputStream: InputStream? = null

    private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //should be before super.onCreate
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btDevices = arrayListOf()
        btnForward = findViewById(R.id.btnForward)
        btnBackward = findViewById(R.id.btnBackward)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnShowAvailableDevices = findViewById(R.id.btnShowAvailableDevices)

        txtSpeed = findViewById(R.id.txtSpeed)
        listView = findViewById(R.id.deviceList)
        rlDistance = findViewById(R.id.rlDistance)
        progressBar = findViewById(R.id.progressBar)
        txtMovement = findViewById(R.id.txtMovement)
        txtDistance = findViewById(R.id.txtDistance)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)
        progressLayout = findViewById(R.id.progressLayout)
        txtObstacleString = findViewById(R.id.txtObstacleString)

        addressList = arrayListOf()
        bluetoothDevices = arrayListOf()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        disableControls()

        btnForward.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                write(11)
                txtMovement.text = "Moving Forward.."
            } else if (event.action == MotionEvent.ACTION_UP) {
                write(10)
                txtMovement.text = "Stopped.."
            }
            true
        }

        btnBackward.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                write(12)
                txtMovement.text = "Moving Backward.."
            } else if (event.action == MotionEvent.ACTION_UP) {
                write(10)
                txtMovement.text = "Stopped.."
            }
            true
        }

        btnLeft.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                write(13)
                txtMovement.text = "Turning Left.."
            } else if (event.action == MotionEvent.ACTION_UP) {
                write(10)
                txtMovement.text = "Stopped.."
            }
            true
        }

        btnRight.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                write(14)
                txtMovement.text = "Turning Right.."
            } else if (event.action == MotionEvent.ACTION_UP) {
                write(10)
                txtMovement.text = "Stopped.."
            }
            true
        }

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevices)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = this

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(broadcastReceiver, intentFilter)

        val pairIntentFilter = IntentFilter()
        pairIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(pairBroadcastReceiver, pairIntentFilter)

        btnShowAvailableDevices.setOnClickListener {
            arrayAdapter.clear()
            arrayAdapter.notifyDataSetChanged()

            if (bluetoothAdapter.isEnabled) {
                bluetoothAdapter.startDiscovery()
                enableProgressView()
                disableUserInteraction()
            } else {
                bluetoothAdapter.enable()
                toastMessageShort("Turning Bluetooth On..")
                Handler(Looper.getMainLooper()).postDelayed({
                    bluetoothAdapter.startDiscovery()
                    enableProgressView()
                    disableUserInteraction()
                }, 2000)
            }
        }

        btnDisconnect.setOnClickListener {
            addressList.clear()
            bluetoothAdapter.disable()
            Handler(Looper.getMainLooper()).postDelayed({
                bluetoothAdapter.enable()
                btnShowAvailableDevices.visibility = View.VISIBLE
                btnDisconnect.visibility = View.GONE
            }, 1000)
        }

        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, index: Int, b: Boolean) {
/*
                when (index) {
                    1 -> speed = 60
                    2 -> speed = 80
                    3 -> speed = 100
                    4 -> speed = 140
                    5 -> speed = 180
                    6 -> speed = 220
                    7 -> speed = 255
                }
*/
                txtSpeed.text = "$index"
                write(index)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                enableUserInteraction()
                disableProgressView()
                if (bluetoothDevices.size != 0)
                    arrayAdapter.notifyDataSetChanged()
                else
                    toastMessageShort("No Device Found")
            } else if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val name = device.name
                    val address = device.address
                    if (!addressList.contains(address)) {
                        addressList.add(address)
                        btDevices.add(device)
                        if (name == "" || name == null)
                            bluetoothDevices.add(address)
                        else
                            bluetoothDevices.add(name)
                    }
                }
            }
        }
    }

    private val pairBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                toastMessageShort("ACTION_BOND_STATE_CHANGED")
                val device: BluetoothDevice? =
                        intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        toastMessageShort("Connected successfully")
                        listView.visibility = View.GONE
                        btnShowAvailableDevices.visibility = View.GONE
                        btnDisconnect.visibility = View.VISIBLE
                    }
                    if (device.bondState == BluetoothDevice.BOND_BONDING) {
                        toastMessageShort("Connecting...")
                    }
                    if (device.bondState == BluetoothDevice.BOND_NONE) {
                        toastMessageShort("Disconnected")
                    }
                }
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        bluetoothAdapter.cancelDiscovery()
//  for BT Classic
        enableProgressView()
        disableUserInteraction()
        val address = btDevices[position].address
        val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
        bluetoothSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(myUUID)
        try {
            bluetoothSocket.connect()
            toastMessageShort("Bluetooth Connected")
            disableProgressView()
            enableUserInteraction()
            listView.visibility = View.GONE
            btnShowAvailableDevices.visibility = View.GONE
            btnDisconnect.visibility = View.VISIBLE
            enableControls()
//            read()
        } catch (socketException: IOException) {
            toastMessageLong("Could not close connection:$socketException")
            Log.i("Socket Exception", "Could not close connection:$socketException")
            disableProgressView()
            enableUserInteraction()
            try {
                bluetoothSocket.close()
                toastMessageShort("Bluetooth Socket Closed")
            } catch (closeException: Exception) {
                toastMessageShort("Could not close connection:$closeException")
            }
            return
        }
//        btDevices[position].createBond()  //for normal devices
    }

    private fun read() {
        val timer = object : Timer() {}
        val timerTask = object : TimerTask() {
            override fun run() {
                inputStream = bluetoothSocket.inputStream
                var input = inputStream?.read()
                var message = (input?.minus(48)).toString()
                txtDistance.text = message
                Log.i("Distance", message)

//                val BUFFER_SIZE = 1024
//                val buffer = ByteArray(BUFFER_SIZE)
//                var bytes = 0
//                val b = BUFFER_SIZE
//                while (true) {
//                    try {
//                        bytes = inputStream?.read(buffer, bytes, BUFFER_SIZE - bytes)!!
//                        Log.i("Distance", "$bytes")
//                        txtDistance.text = bytes.toString()
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//                inputStream = bluetoothSocket.inputStream
//                inputStream?.skip(inputStream?.available()!!.toLong())
//                if (inputStream?.available()!! > 0) {
//                    var input = inputStream?.read()
//                    var message = (input?.minus(48)).toString()
//                    txtDistance.text = message
//                }
            }
        }
        timer.schedule(timerTask, 2000, 1000)
    }

    private fun write(data: Int) {
        if (bluetoothSocket != null) {
            bluetoothSocket.outputStream.write(data)
        }
    }

    private fun toastMessageShort(string: String) {
        Toast.makeText(applicationContext, string, Toast.LENGTH_SHORT).show()
    }

    private fun toastMessageLong(string: String) {
        Toast.makeText(applicationContext, string, Toast.LENGTH_LONG).show()
    }

    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun enableUserInteraction() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private fun enableProgressView() {
        progressLayout.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun disableProgressView() {
        progressLayout.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun disableControls() {
        btnLeft.visibility = View.GONE
        btnRight.visibility = View.GONE
        btnForward.visibility = View.GONE
        btnBackward.visibility = View.GONE
    }

    private fun enableControls() {
        btnLeft.visibility = View.VISIBLE
        btnRight.visibility = View.VISIBLE
        btnForward.visibility = View.VISIBLE
        btnBackward.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver(pairBroadcastReceiver)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (// View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
}