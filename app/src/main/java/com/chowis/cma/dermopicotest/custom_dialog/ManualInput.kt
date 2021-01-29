package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.Color
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Range
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.chowis.cma.dermopicotest.R
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.controls.WhiteBalance
import kotlinx.android.synthetic.main.layout_dialog_manual_input.*

class ManualInput(
    context: Context, private val listener: Listener
): BaseDialog(context) {
    private lateinit var btn100: Button
    private lateinit var btn200: Button
    private lateinit var btn400: Button
    private lateinit var btn800: Button
    private lateinit var btn1600: Button
    private lateinit var btn3200: Button
    
    private lateinit var btnWBAuto: ImageView
    private lateinit var btnWBIncandescent: ImageView
    private lateinit var btnWBFluorescent: ImageView
    private lateinit var btnWBDaylight: ImageView
    private lateinit var btnWBCloudy:ImageView
    
    private lateinit var btnDone: Button

    private var iso_value = 400
    private var wb_value: WhiteBalance = WhiteBalance.AUTO

    private var selectedISO: View? = null
    private var selectedWB: View ? = null

    private lateinit var cameraOptions: CameraOptions

    override val layoutId: Int = R.layout.layout_dialog_manual_input
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()

    }

    private fun init() {
        btn100 = findViewById(R.id.iso100)
        btn200 = findViewById(R.id.iso200)
        btn400 = findViewById(R.id.iso400)
        btn800 = findViewById(R.id.iso800)
        btn1600 = findViewById(R.id.iso1600)
        btn3200 = findViewById(R.id.iso3200)
        
        btnWBAuto = findViewById(R.id.btnWBAuto)
        btnWBIncandescent = findViewById(R.id.btnWBIncandescent)
        btnWBFluorescent = findViewById(R.id.btnWBFluorescent)
        btnWBDaylight = findViewById(R.id.btnWBDaylight)
        btnWBCloudy = findViewById(R.id.btnWBCloudy)
        
        btnDone = findViewById(R.id.btnDone)

        btnDone.setOnClickListener {
            listener.cameraChanges(
                wb_value,
                iso_value,
                focusSlider.progress,
                shutterSlider.progress,
                exposureValue()
            )
            dismiss()
        }

        selector()
    }


    private fun exposureValue(): Float {
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0]
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val range1: Range<Int>? =
            characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val minExposure = range1?.lower
        val maxExposure = range1?.upper
        val sliderValue = ((exposureSlider.progress / 25) - 2).toFloat()
        var exposureValue: Float = 0f
        if (minExposure != null && maxExposure != null) {
            if (minExposure != null) {
                exposureValue =
                    if (sliderValue >= 0) {
                            (minExposure * sliderValue)
                    } else {
                            (maxExposure * -1 * sliderValue)
                    }
                }
            }
        return exposureValue
    }

    private fun selector() {
        btn100.setOnClickListener { setISO(it) }
        btn200.setOnClickListener { setISO(it) }
        btn400.setOnClickListener { setISO(it) }
        btn800.setOnClickListener { setISO(it) }
        btn1600.setOnClickListener { setISO(it) }
        btn3200.setOnClickListener { setISO(it) }
        
        btnWBAuto.setOnClickListener { setWB(it) }
        btnWBIncandescent.setOnClickListener { setWB(it) }
        btnWBFluorescent.setOnClickListener { setWB(it) }
        btnWBDaylight.setOnClickListener { setWB(it) }
        btnWBCloudy.setOnClickListener { setWB(it) }
    }

    private fun setWB(v: View) {
        selectedWB?.setBackgroundResource(0)
        when (v.id) {
            R.id.btnWBAuto -> wb_value = WhiteBalance.AUTO
            R.id.btnWBIncandescent -> wb_value = WhiteBalance.INCANDESCENT
            R.id.btnWBFluorescent -> wb_value = WhiteBalance.FLUORESCENT
            R.id.btnWBDaylight -> wb_value = WhiteBalance.DAYLIGHT
            R.id.btnWBCloudy -> wb_value = WhiteBalance.CLOUDY
        }
        v.setBackgroundResource(R.drawable.border)
        selectedWB = v
    }

    private fun setISO(v: View) {
        selectedISO?.setBackgroundResource(android.R.drawable.btn_default)
        when (v.id) {
            R.id.iso100 -> iso_value = 100
            R.id.iso200 -> iso_value = 200
            R.id.iso400 -> iso_value = 400
            R.id.iso800 -> iso_value = 800
            R.id.iso1600 -> iso_value = 1600
            R.id.iso3200 -> iso_value = 3200
        }
        v.setBackgroundColor(Color.BLUE)
        selectedISO = v
    }

    interface Listener {
        fun cameraChanges(
            whiteBalance: WhiteBalance,
            iso: Int,
            focus: Int,
            shutterSpeed: Int,
            exposure: Float
        )
    }
}


