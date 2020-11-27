package com.chowis.cma.dermopicotest.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.ble.BleConnectionActivity
import com.chowis.cma.dermopicotest.custom_dialog.CalibratingDialog
import com.chowis.cma.dermopicotest.custom_dialog.NoticeDialog
import com.chowis.cma.dermopicotest.db.DatabaseBuilder
import com.chowis.cma.dermopicotest.db.DatabaseHelperImpl
import com.chowis.cma.dermopicotest.model.Calibrate
import com.chowis.cma.dermopicotest.util.*
import com.chowis.cma.dermopicotest.view_model.AnalyzeViewModel
import com.chowis.jniimagepro.JNIImageProCW
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.size.AspectRatio
import kotlinx.android.synthetic.main.activity_analyze.*
import kotlinx.android.synthetic.main.layout_header.*
import kotlinx.coroutines.*
import permissions.dispatcher.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AnalyzeActivity : BleConnectionActivity() {
    private lateinit var calibratingDialog: Dialog
    private lateinit var noticeDialog: Dialog
    private var mCurrentPicturePath: String = ""
    private var mCurrentPicturePathResult: String = ""
    private val imagePath: String = Constants.IMAGE_TEMP_PATH
    private var menu: String = ""
    private lateinit var vm: AnalyzeViewModel
    private val NOMEDIA_FILE = ".nomedia"

    override fun onGetContentViewResource(): Int {
        menu = intent.getStringExtra(Constants.MENU)!!
        return if (menu == Constants.MENU_HAIR) R.layout.activity_analyze else R.layout.activity_analyze_skin
    }

    override fun onInit() {
        menu = intent.getStringExtra(Constants.MENU)!!

        showCamera()
        iv_image_capture.setOnClickListener {
            takePicture()
        }
        btn_cancel.setOnClickListener { onBackPressed() }
        btn_retake.setOnClickListener {
            retakePicture()
        }
        btn_analyze.setOnClickListener {
            onAnalyze()
        }
        iv_back.setOnClickListener { onBackPressed() }
        iv_ble.setOnClickListener { isBluetoothConnected() }
        focus_view1.canvassColor = getColor(resources, R.color.colorWhite, null)

        vm = ViewModelProviders.of(
            this,
            ViewModelFactory(
                DatabaseHelperImpl(DatabaseBuilder.getInstance(applicationContext))
            )
        ).get(AnalyzeViewModel::class.java)

    }

    private fun showCalibratingDeviceDialog() {
        calibratingDialog = CalibratingDialog(this, object : CalibratingDialog.Listener {
            override fun onProceed() {
                SharedPref.isFirstUse = false
                SharedPref.newDevice = false
            }
        })
        calibratingDialog.show()
    }
    private fun retakePicture(){
        iv_image_capture.visibility = View.VISIBLE
        img_original.visibility = View.INVISIBLE
        mCurrentPicturePath = ""
        mCurrentPicturePathResult = ""
        showAnalyses(8)
    }
    private fun showNoticeDialog(message: String, buttonText: String, resId: Int, visibility: Int) {

        noticeDialog = NoticeDialog(
            this,
            resId,
            buttonText,
            message,
            visibility,
            object : NoticeDialog.Listener {
                override fun onOkRetry() {
                    if (buttonText == "Retry") {
                        retakePicture()
                    }else{
                        onBackPressed()
                    }
                }

                override fun onCancel() {
                }
            })
        noticeDialog.show()

    }


    fun showCamera() {
        if (SharedPref.newDevice || SharedPref.isFirstUse) {
            showCalibratingDeviceDialog()
        }
        camera.setLifecycleOwner(this)
        camera.setFocusMode(2)
        camera.zoom = .3f
        camera.useDeviceOrientation = false
        camera.setPictureSize(CameraUtil.getOptimalPictureSize(1080, 1440, AspectRatio.of(3, 4)))
        camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)
                val ratio = AspectRatio.of(result.size)
                Timber.d("result size: ${result.size}, ratio: $ratio")
                Timber.d("className=$mCurrentPicturePath")

                val outputFile = File(mCurrentPicturePath)
                result.toFile(outputFile) {
                    processPicture()
                }
            }
        })

    }

    override fun onResume() {
        super.onResume()
        scanBleDevice()
    }

    private fun onAnalyze() {
        var sizeFactor = 0.0
        CoroutineScope(Dispatchers.IO).launch {

            withContext(Dispatchers.Main) {
                showNoticeDialog(getString(R.string.please_wait), "", R.drawable.loading, View.GONE)
            }

            runBlocking {
                val orgPath = mCurrentPicturePath
                val resultPath = mCurrentPicturePathResult

                val imgPro = JNIImageProCW()
                var rawValue = imgPro.CMACalibJni(orgPath, resultPath)
                sizeFactor = rawValue
                if(sizeFactor > 0){
                    val calibrate = Calibrate(
                        0,
                        "image",
                        orgPath,
                        resultPath,
                        CommonUtil.getCurrenTime(),
                        CommonUtil.getCurrentDate(),
                        menu,
                        "" + sizeFactor
                    )
                    Timber.d("rawValue=$rawValue")
                    val file = File(Constants.PICO_PATH, NOMEDIA_FILE)

                    if (!file.exists()) {
                        try {
                            file.createNewFile()
                        } catch (e: IOException) {
                            Timber.e("IOException" + e.localizedMessage)
                        }
                    }

                    sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(File(resultPath))
                        )
                    )
                    // Save to Database
                    vm.insetCalibration(calibrate)
                }

            }
            withContext(Dispatchers.Main) {
                if (noticeDialog.isShowing)
                    noticeDialog.dismiss()
                if (sizeFactor > 0) showNoticeDialog(
                    getString(R.string.notice_success),
                    "Ok",
                    R.drawable.success,
                    View.VISIBLE
                )
                else showNoticeDialog(
                    getString(R.string.notice_error),
                    "Retry",
                    R.drawable.error,
                    View.VISIBLE
                )
            }
        }
    }

    private fun getFullImagePath(timeStamp: String): kotlin.String? {

        return imagePath + java.io.File.separator + timeStamp + "cma_camera.jpg"
    }

    private fun getFullResultImagePath(timeStamp: String): kotlin.String? {
        return imagePath + java.io.File.separator + timeStamp + "cma_camera_result.jpg"
    }



    private fun showAnalyses(visibility: Int) {
        btn_analyze.visibility = visibility
        btn_retake.visibility = visibility
    }

    private fun takePicture() {
        showProgressDialog()
        iv_image_capture.isEnabled = false
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        mCurrentPicturePath = this.getFullImagePath(timeStamp).toString()
        mCurrentPicturePathResult = this.getFullResultImagePath(timeStamp).toString()
        camera.takePicture()
        Timber.d("end")
    }



    private fun processPicture() {
        hideProgressDialog()
        camera.visibility = View.INVISIBLE
        img_original.visibility = View.INVISIBLE
        iv_camera_guide.visibility = View.INVISIBLE

        showNoticeDialog(getString(R.string.please_wait_picture), "", R.drawable.loading, View.GONE)
        CoroutineScope(Dispatchers.IO).launch {
            runBlocking {
                CameraUtil.resizeBitmap(mCurrentPicturePath,menu)
            }

            withContext(Dispatchers.Main) {
                Timber.d("processPicture end")
                Glide.with(this@AnalyzeActivity).load(mCurrentPicturePath).into(img_original)
                //delay 2 secs to not show the white background after dialog dismiss
                delay(2000)
                noticeDialog.dismiss()
                camera.visibility = View.VISIBLE
                img_original.visibility = View.VISIBLE
                iv_camera_guide.visibility = View.VISIBLE
                iv_image_capture.isEnabled = true
                iv_image_capture.visibility = View.GONE
                showAnalyses(1)

            }
        }
    }

}