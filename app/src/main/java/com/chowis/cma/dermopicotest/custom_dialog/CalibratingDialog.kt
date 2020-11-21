package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.chowis.cma.dermopicotest.R
import kotlinx.android.synthetic.main.layout_dialog_view_image.*

class CalibratingDialog(
    context: Context,
    private val listener: Listener
) : BaseDialog(context) {
    override val layoutId: Int = R.layout.layout_dialog_calibrating

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnProceed = findViewById<Button>(R.id.btn_dialog_proceed)

        btnProceed.setOnClickListener {
            dismiss()
            listener.onProceed()
        }
        txtBack.setOnClickListener {
            dismiss()
        }
    }

    interface Listener {
        fun onProceed()
    }
}
