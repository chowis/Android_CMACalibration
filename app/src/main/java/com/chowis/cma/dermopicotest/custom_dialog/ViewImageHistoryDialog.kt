package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.bumptech.glide.Glide
import com.chowis.cma.dermopicotest.R
import kotlinx.android.synthetic.main.activity_analyze.*
import kotlinx.android.synthetic.main.layout_dialog_view_image.*
import timber.log.Timber

class ViewImageHistoryDialog(
    context: Context, private val path: String,private val sizeFactor: String
) : BaseDialog(context) {
    override val layoutId: Int = R.layout.layout_dialog_view_image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Glide.with(context).load(path).into(iv_image_history)
        txtBack.setOnClickListener {
            dismiss()
        }
        tv_size_factor.text = "Size Factor: $sizeFactor"

    }
}
