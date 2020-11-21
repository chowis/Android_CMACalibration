package com.chowis.cma.dermopicotest.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.util.SharedPref

import kotlinx.coroutines.*
import timber.log.Timber

abstract class BleConnectionActivity : AppCompatActivity() {
    private val className = javaClass.simpleName
    private val bleDevices = mutableListOf<BluetoothDevice>()
    private val handler = Handler()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    protected var mustPairWithADevice = false
    private var isBleServiceBound = false
    private var isWaitingForBleAdapterStateOn = false // bec BluetoothAdapter.isEnabled is not updated immediately
    private var isBleScanStarted = false // bec BluetoothAdapter.isDiscovering is not updated immediately

    private lateinit var progressDialog: Dialog
    private lateinit var loadingDialog: Dialog

    companion object {
        const val REQUEST_ENABLE_BT = 0 // must not be the same with any request code of derived class
        const val BLE_SCAN_TIMEOUT: Long = 20000 // 20 seconds
    }

    protected abstract fun onGetContentViewResource(): Int
    protected abstract fun onInit()
    open fun onReceiveBleWriteComplete() {}
    open fun onReceiveBleMoistureData(moistureData: Int) {}
    open fun onBleScanCompleted() {}

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("className=$className")
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        setupLoadingDialog()

        val bleServiceIntent = Intent(this, BleService::class.java)
        isBleServiceBound = bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE)
        Timber.d("isBleServiceBound=$isBleServiceBound")

        val intentFilter = getGattIntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(broadcastReceiver, intentFilter)

        setContentView(onGetContentViewResource())
//        updateLocale(this, SharedPref.selectedLanguage)

//        updateCloudIcon()
//        setupBackButtonListeners()
//        setupScanningForDeviceDialog()
//        setupDiscoveredDevicesDialog()
//        setupTurnOnDeviceDialog()
        setupProgressDialog()

        onInit()
    }

    override fun onStart() {
        Timber.d("className=$className")
        super.onStart()
    }

    override fun onResume() {
        Timber.d("className=$className")
        super.onResume()
    }

    override fun onPause() {
        Timber.d("className=$className")
        super.onPause()
    }

    override fun onStop() {
        Timber.d("className=$className")
        super.onStop()
    }

    protected open fun getGattIntentFilter(): IntentFilter {
        Timber.d("className=$className")
        val intentFilter = IntentFilter()
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BleService.ACTION_BATTERY_LEVEL_CHANGED)
        return intentFilter
    }

    fun scanBleDevice() {
        Timber.d("className=$className, bluetoothAdapter.isEnabled=${bluetoothAdapter.isEnabled}, isWaitingForBleAdapterStateOn=${isWaitingForBleAdapterStateOn}")
        if (!bluetoothAdapter.isEnabled) {
            if (!isWaitingForBleAdapterStateOn) {
                isWaitingForBleAdapterStateOn = true
                askPermissionToEnableBluetooth()
            }
        } else {
            startBleScan()
        }
    }
    fun isBluetoothConnected() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show()

            if (!isWaitingForBleAdapterStateOn) {
                isWaitingForBleAdapterStateOn = true
                askPermissionToEnableBluetooth()
            }
        } else if(isBleConnected()) {
            Toast.makeText(this, "Bluetooth is connected", Toast.LENGTH_SHORT).show()

        }else if(isBleScanStarted) {
            Toast.makeText(this, "Bluetooth is scanning", Toast.LENGTH_SHORT).show()

        }else if(bleDevices.isEmpty()){
            Toast.makeText(this, "Unable to connect to device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askPermissionToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("className=$className")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // BluetoothAdapter may still not be enabled at this point so we have to wait for
                // it at the broadcastReceiver under BluetoothAdapter.ACTION_STATE_CHANGED
                return
            } else {
                // user did not choose to enable bluetooth so we just loop to ask again
                askPermissionToEnableBluetooth()
            }
        }
    }

    private fun startBleScan() {
        Timber.d("className=$className")
        if (isBleConnected()) {
            return
        }

        if (!isBleScanStarted) {
            isBleScanStarted = true
            bleDevices.clear()

            Timber.d("Showing user that scanning is ongoing...")

            // Stop scanning after a predefined scan period.
            handler.postDelayed(stopBleScanRunnable, BLE_SCAN_TIMEOUT)

            Timber.d("className=$className, Starting BLE scan...")
            bluetoothAdapter.startLeScan(bleScanCallback)
        }
    }

    private fun stopBleScan() {
        isBleScanStarted = false
        bluetoothAdapter.stopLeScan(bleScanCallback)
    }

    private val stopBleScanRunnable = Runnable {
        Timber.d("className=$className, Stopping BLE scan (due to timeout)...")
        stopBleScan()

        if (bleDevices.isEmpty()) {
            Timber.d("Instructing user to turn on device...")
            // This dialog will not be shown in stop/paused state (e.g. screen timeout).
            Toast.makeText(this, R.string.turn_on_device, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelBleScan() {
        Timber.d("className=$className")
        stopBleScan()
        handler.removeCallbacks(stopBleScanRunnable)
        onBleScanCompleted()
    }

    private val bleScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        Timber.d("className=$className, Found a BLE device: name=${device.name}, address:${device.address}")
        if (device.name != null && device.name.startsWith("CMA")) {
            Timber.d("className_ble=$bleService")

            if (bleService != null && SharedPref.pairedDevice == device.address) {
                Timber.d("Found a previously bonded/paired device. Canceling BLE scan...")
                cancelBleScan()
                if (bleService?.connect(device.address)!!) {
                    Timber.d("Successfully initiated connect.")
                } else {
                    Timber.d("Unable to initiate connect.")
                }
            } else {
                val bleDevice = bleDevices.find {
                    device.address == it.address
                }
                if (bleService?.connect(device.address)!!) {
                    Timber.d("Successfully initiated connect.")
                    SharedPref.pairedDevice = device.address
                    SharedPref.pairedDeviceName = device.name
                    SharedPref.newDevice = true
                } else {
                    Timber.d("Unable to initiate connect.")
                }

//                if (bleDevice == null) {
//                    bleDevices.add(device)
//                }
            }
        }
    }
    private fun showDialog(dialog: Dialog) {
        // Make sure activity is active/running before showing dialog to prevent crashes.
        if (!isFinishing)
            dialog.show()
    }

    protected fun showProgressDialog() {
        showDialog(progressDialog)
    }

    protected fun hideProgressDialog() {
        progressDialog.dismiss()
    }

    private fun setupProgressDialog() {
        progressDialog = Dialog(this)
        /*val imgLoadingGif = ImageView(this)
        Glide.with(this)
                .load(R.drawable.loading)
                .override(200, 200)
                .into(imgLoadingGif)*/

        var layout = layoutInflater.inflate(R.layout.loading_progress, null) as ConstraintLayout
        progressDialog.setContentView(layout)
        progressDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog.setCancelable(false)
        //progressDialog.lottieAnimationView.setAnimation("spinning_circular_lines.json")
        //progressDialog.lottieAnimationView.playAnimation()
    }

    private var bleService: BleService? = null
    private val bleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleService = (service as BleService.LocalBinder).service
            Timber.d("className=$className, bleService=$bleService")

            if (bleService?.connect(SharedPref.pairedDevice)!!) {
                Timber.d("Successfully initiated connect.")
            } else {
                Timber.d("Unable to initiate connect.")
            }
            val batteryLevel = bleService?.batteryLevel!!
            Timber.d("batteryLevel=$batteryLevel")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("className=$className, bleService=null")
            bleService = null
        }
    }

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: ""

            Timber.d("className=$className, action=$action")
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val bleAdapterState = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    Timber.d("bleAdapterState=$bleAdapterState, isWaitingForBleAdapterStateOn=$isWaitingForBleAdapterStateOn")
                    if (bleAdapterState == BluetoothAdapter.STATE_ON && isWaitingForBleAdapterStateOn) {
                        Timber.d("BluetoothAdapter is now enabled.")
                        isWaitingForBleAdapterStateOn = false
                        startBleScan()
                    }
                }
                BleService.ACTION_GATT_CONNECTED -> {
                    // This action is received once e.g. in startup activity (CheckActivity).
                    // So all other activities receive the connected state at onServiceConnected().
                    runOnUiThread {
                        if (!mustPairWithADevice) {
                            Timber.d("Canceling BLE scan (gatt already connected)...")
                            cancelBleScan()
                        }
//                        updateBleIcon()
                    }
                }
                BleService.ACTION_GATT_DISCONNECTED -> {
                    runOnUiThread {
//                        updateBleIcon()
                    }
                }
                BleService.ACTION_CHARACTERISTIC_WRITE_COMPLETE -> {
                    hideProgressDialog()
                    onReceiveBleWriteComplete()
                }
                BleService.ACTION_CHARACTERISTIC_WRITE_ERROR -> {
                    hideProgressDialog()
                    onReceiveBleWriteComplete()
                }
                BleService.ACTION_BATTERY_LEVEL_CHANGED -> {
                    runOnUiThread {
//                        updateBleBatteryLevel()
                    }
                }
                BleService.ACTION_MOISTURE_DATA_AVAILABLE -> {
                }
            }
        }
    }

    private fun isBleConnected(): Boolean {
        Timber.d("className=$className")
        if (bleService == null) {
            return false
        }
        return bleService?.isConnected!!
    }

    fun executeBleWrite(bleMode: BleMode) {
        Timber.d("className=$className, bleMode=${bleMode.name}")
        if (!isBleConnected()) {
            return
        }

        if (!bleService?.isChangeModeNeeded(bleMode)!!)
            return

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                showProgressDialog()
            }

            runBlocking {
                bleService?.writeCharacteristic(bleMode)
            }
        }
    }

    override fun onDestroy() {
        Timber.d("className=$className, isBleServiceBound=$isBleServiceBound, bleService=$bleService")
        unregisterReceiver(broadcastReceiver)
        if (isBleServiceBound) {
            unbindService(bleServiceConnection)
            bleService = null
        }
        super.onDestroy()
    }
    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        val layout = layoutInflater.inflate(R.layout.loading_progress, null)
        loadingDialog.setContentView(layout)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.setCancelable(false)
    }

    fun showLoadingDialog() {
        Timber.d(" ")
        if (!isFinishing)
            loadingDialog.show()
    }

    fun hideLoadingDialog() {
        Timber.d(" ")
        loadingDialog.dismiss()
    }


}