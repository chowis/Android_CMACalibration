package com.chowis.cma.dermopicotest.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.ble.BleConnectionActivity
import com.chowis.cma.dermopicotest.custom_dialog.MenuDialog
import com.chowis.cma.dermopicotest.util.CommonUtil
import com.chowis.cma.dermopicotest.util.Constants
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import timber.log.Timber
import java.io.File
import java.io.IOException


@RuntimePermissions
class MainActivity : BleConnectionActivity() {
    private lateinit var menuDialog: Dialog
    private val NOMEDIA_FILE = ".nomedia"
    override fun onGetContentViewResource(): Int {
        return R.layout.activity_main
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)

    }

    @OnPermissionDenied(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE

    )
    fun onPermissionsDenied() {
        finish()
    }

    @OnShowRationale(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE

    )
    fun showRationale(request: PermissionRequest) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        showRationaleDialog(R.string.rationale, request)
    }

    private fun showRationaleDialog(@StringRes messageResId: Int, request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setPositiveButton(R.string.allow) { _, _ -> request.proceed() }
            .setNegativeButton(R.string.deny) { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage(messageResId)
            .show()
    }

    @NeedsPermission(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE

    )
    open fun askPermissions() {
        createDirectories()
    }
    @SuppressLint("NewApi")
    private fun createDirectory(path: File) {
        if (!path.exists()) {
            path.mkdirs()
            val DIR_FORMAT = 0x3001
            val MediaUri = MediaStore.Files.getContentUri("external")
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DATA, path.absolutePath)
            values.put("format", DIR_FORMAT)
            values.put(
                MediaStore.MediaColumns.DATE_MODIFIED,
                System.currentTimeMillis() / 1000
            )
            applicationContext.contentResolver.insert(MediaUri, values)
        }
    }

    private fun createDirectories() {
        createDirectory(File(Constants.PICO_PATH))
        createDirectory(File(Constants.IMAGE_PATH))
        createDirectory(File(Constants.IMAGE_TEMP_PATH))
        val file = File(Constants.PICO_PATH, NOMEDIA_FILE)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    override fun onInit() {
        askPermissionsWithPermissionCheck()
        btn_calibrate.setOnClickListener { showMenu() }
        btn_view_history.setOnClickListener { showHistory() }
        iv_main_ble.setOnClickListener { isBluetoothConnected() }
        tv_app_version.text = appVersion()
    }

    private fun appVersion(): String {
        return getString(R.string.ver) + " " + CommonUtil.getAppVersion(this)
    }

    override fun onResume() {
        super.onResume()
        scanBleDevice()
    }

    private fun showMenu() {
        menuDialog = MenuDialog(this, object : MenuDialog.Listener {
            val intent = Intent(applicationContext, AnalyzeActivity::class.java)
            override fun onShowSkin() {
                intent.putExtra(Constants.MENU, Constants.MENU_SKIN)
                startActivity(intent)
            }

            override fun onShowHair() {
                intent.putExtra(Constants.MENU, Constants.MENU_HAIR)
                startActivity(intent)
            }
        })
        menuDialog.show()
    }

    private fun showHistory() {
        val intent = Intent(applicationContext, SaveImageHistoryActivity::class.java)
        startActivity(intent)
    }
}