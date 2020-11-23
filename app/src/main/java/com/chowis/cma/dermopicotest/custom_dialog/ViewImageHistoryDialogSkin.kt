package com.chowis.cma.dermopicotest.custom_dialog

import android.content.Context
import android.os.Bundle
import com.bumptech.glide.Glide
import com.chowis.cma.dermopicotest.R
import kotlinx.android.synthetic.main.layout_dialog_view_image.*
import kotlinx.android.synthetic.main.layout_dialog_view_image.tv_size_factor
import kotlinx.android.synthetic.main.layout_dialog_view_image.txtBack
import kotlinx.android.synthetic.main.layout_dialog_view_image_skin.*

class ViewImageHistoryDialogSkin(
    context: Context, private val path: String, private val sizeFactor: String
) : BaseDialog(context) {
    override val layoutId: Int = R.layout.layout_dialog_view_image_skin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Glide.with(context).load(path).into(iv_image_history_skin)
        txtBack.setOnClickListener {
            dismiss()
        }
        tv_size_factor.text = "Size Factor: $sizeFactor"
    }
}
