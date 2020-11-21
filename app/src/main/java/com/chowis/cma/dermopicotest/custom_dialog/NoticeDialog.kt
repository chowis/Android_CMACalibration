package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.chowis.cma.dermopicotest.R
import kotlinx.android.synthetic.main.layout_dialog_view_image.*

class NoticeDialog(
    context: Context,
    private val resId: Int,
    private val buttonText: String,
    private val message: String,
    private val okRetryVisibility :Int,
    private val listener: Listener
) : BaseDialog(context) {
    override val layoutId: Int = R.layout.layout_dialog_noitice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val btnCancel = findViewById<Button>(R.id.btn_dialog_cancel)
        btnCancel.setOnClickListener {
            dismiss()
            listener.onCancel()
        }
        val btnOkRetry = findViewById<Button>(R.id.btn_dialog_ok_retry)
        btnOkRetry.text = buttonText
        btnOkRetry.visibility = okRetryVisibility
        btnOkRetry.setOnClickListener {
            dismiss()
            listener.onOkRetry()
        }

        val tvMessage= findViewById<TextView>(R.id.tv_message)
        tvMessage.text = message

        val ivIcon= findViewById<ImageView>(R.id.iv_notice_icon)
        ivIcon.setImageResource(resId)

    }

    interface Listener {
        fun onOkRetry()
        fun onCancel()
    }
}
