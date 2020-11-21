package com.chowis.cma.dermopicotest.activity

import android.app.Dialog
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.adapter.HistoryAdapter
import com.chowis.cma.dermopicotest.ble.BleConnectionActivity
import com.chowis.cma.dermopicotest.custom_dialog.ViewImageHistoryDialog
import com.chowis.cma.dermopicotest.custom_dialog.ViewImageHistoryDialogSkin
import com.chowis.cma.dermopicotest.db.DatabaseBuilder
import com.chowis.cma.dermopicotest.db.DatabaseHelperImpl
import com.chowis.cma.dermopicotest.model.Calibrate
import com.chowis.cma.dermopicotest.util.ClickListener
import com.chowis.cma.dermopicotest.util.Constants
import com.chowis.cma.dermopicotest.util.ViewModelFactory
import com.chowis.cma.dermopicotest.view_model.AnalyzeViewModel
import kotlinx.android.synthetic.main.activity_saved_image_history.*
import kotlinx.android.synthetic.main.layout_header.*
import timber.log.Timber

class SaveImageHistoryActivity : BleConnectionActivity(), ClickListener {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyImageDialog: Dialog
    private lateinit var historyImageDialogSkin: Dialog
    private lateinit var vm: AnalyzeViewModel
    private var path: String = ""
    private var isCheck: Boolean = false
    private var type: String = ""
    private var sizeFactor: String = ""
    private  var checked : MutableList<Boolean> = mutableListOf()
    override fun onGetContentViewResource(): Int {
        return R.layout.activity_saved_image_history
    }

    override fun onInit() {
        tv_title.visibility = View.VISIBLE
        linearLayoutManager = LinearLayoutManager(this)
        rv_history.layoutManager = linearLayoutManager

        btn_view_image.setOnClickListener {
            if (type == Constants.MENU_HAIR) showHistoryImage(path, isCheck, sizeFactor) else showHistoryImageSkin(path, isCheck,sizeFactor)
        }
        iv_ble.setOnClickListener { isBluetoothConnected() }
        iv_back.setOnClickListener { onBackPressed() }
        vm = ViewModelProviders.of(
            this,
            ViewModelFactory(
                DatabaseHelperImpl(DatabaseBuilder.getInstance(applicationContext))
            )
        ).get(AnalyzeViewModel::class.java)
        getCalibrationHistory()
    }

    private fun showHistoryImage(pathImage: String, isChecked: Boolean,sizeFactor : String) {
        if (checked.size == 1) {
            historyImageDialog = ViewImageHistoryDialog(this, pathImage,sizeFactor)
            historyImageDialog.show()
        }
        else{
            Toast.makeText(this, "Please select only 1 image to view", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHistoryImageSkin(pathImage: String, isChecked: Boolean,sizeFactor : String) {
        if (checked.size == 1) {
            historyImageDialogSkin = ViewImageHistoryDialogSkin(this, pathImage,sizeFactor)
            historyImageDialogSkin.show()
        }else{
            Toast.makeText(this, "Please select only 1 image to view", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun getCalibrationHistory() {
        vm.calibrateToDb()
        vm.getCalibration().observe(this, Observer<List<Calibrate>> {
            adapter = HistoryAdapter(it, this)
            rv_history.adapter = adapter
        })
    }

    override fun onClickListener(isChecked: Boolean, history: Calibrate) {
        Timber.d("onClickListener")
        if (isChecked) {
            path = history.imagePath
            isCheck = isChecked
            this.type = history.type
            this.sizeFactor = history.sizeFactor
        }
        if (isChecked) checked.add(true) else checked.remove(true)
        if (checked.size == 0) {
            btn_view_image.setBackgroundResource(R.drawable.rounded_gray_button_disabled)
            btn_view_image.isEnabled = false
        }else{
            btn_view_image.setBackgroundResource(R.drawable.rounded_gray_button)
            btn_view_image.isEnabled = true
        }
    }
}