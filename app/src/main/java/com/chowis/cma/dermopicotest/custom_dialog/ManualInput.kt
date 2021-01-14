package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import com.chowis.cma.dermopicotest.R
import com.otaliastudios.cameraview.controls.WhiteBalance
import java.util.*

class ManualInput(
    context: Context, private val listener: Listener
): BaseDialog(context) {
    private var focusSlider: SeekBar? = null
    private var shutterSlider:SeekBar? = null
    private var exposureSlider:SeekBar? = null

    private lateinit var btn100: Button
    private lateinit var btn200: Button
    private lateinit var btn400: Button
    private lateinit var btn800: Button
    private lateinit var btn1600: Button
    private lateinit var btn3200: Button
    
    private lateinit var btnWBAuto: Button
    private lateinit var btnWBIncandescent: Button
    private lateinit var btnWBFluorescent: Button
    private lateinit var btnWBDaylight: Button
    private lateinit var btnWBCloudy:Button
    
    private lateinit var btnDone: Button

    private var iso_value = 400
    private var wb_value: WhiteBalance = WhiteBalance.AUTO

    private var selectedISO: View? = null

    override val layoutId: Int = R.layout.layout_dialog_manual_input
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {

        focusSlider = findViewById(R.id.focusSlider)
        shutterSlider = findViewById(R.id.shutterSlider)
        exposureSlider = findViewById(R.id.exposureSlider)

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
            listener.cameraChanges(wb_value,iso_value)
        }
        
        isoSelector()

    }

    private fun isoSelector() {
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
        when (v.id) {
            R.id.btnWBAuto -> wb_value = WhiteBalance.AUTO
            R.id.btnWBIncandescent -> wb_value = WhiteBalance.INCANDESCENT
            R.id.btnWBFluorescent -> wb_value = WhiteBalance.FLUORESCENT
            R.id.btnWBDaylight -> wb_value = WhiteBalance.DAYLIGHT
            R.id.btnWBCloudy -> wb_value = WhiteBalance.CLOUDY
        }
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
        fun cameraChanges(whiteBalance: WhiteBalance, iso: Int)
    }
}


