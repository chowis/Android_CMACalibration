package com.chowis.cma.dermopicotest.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.ble.BleConnectionActivity
import com.chowis.cma.dermopicotest.custom_dialog.MenuDialog
import com.chowis.cma.dermopicotest.util.CommonUtil
import com.chowis.cma.dermopicotest.util.Constants
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BleConnectionActivity() {
    private lateinit var menuDialog: Dialog
    override fun onGetContentViewResource(): Int {
    return R.layout.activity_main
    }

    override fun onInit() {
        btn_calibrate.setOnClickListener { showMenu() }
        btn_view_history.setOnClickListener { showHistory() }
        iv_main_ble.setOnClickListener { isBluetoothConnected() }
        tv_app_version.text = appVersion()
    }
    private fun appVersion (): String{
        return getString(R.string.ver) +" "+CommonUtil.getAppVersion(this)
    }
    override fun onResume() {
        super.onResume()
        scanBleDevice()
    }
    private fun showMenu(){
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