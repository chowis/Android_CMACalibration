package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.chowis.cma.dermopicotest.R
import kotlinx.android.synthetic.main.layout_dialog_view_image.*

class MenuDialog(
    context: Context,
    private val listener: Listener
) : BaseDialog(context) {
    override val layoutId: Int = R.layout.layout_dialog_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnSkin = findViewById<Button>(R.id.btn_dialog_skin)

        btnSkin.setOnClickListener {
            dismiss()
            listener.onShowSkin()
        }
        txtBack.setOnClickListener {
            dismiss()
        }
        val btnHair = findViewById<Button>(R.id.iv_image_history)

        btnHair.setOnClickListener {
            dismiss()
            listener.onShowHair()
        }
    }

    interface Listener {
        fun onShowSkin()
        fun onShowHair()
    }
}
