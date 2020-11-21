package com.chowis.cma.dermopicotest.custom_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class FocusView : View {

    var bm: Bitmap? = null
    var cv: Canvas? = null
    var eraser: Paint? = null
    var canvassColor: Int = Color.TRANSPARENT

    constructor(context: Context?) : super(context) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initPaints()
    }

    private fun initPaints() {
        eraser = Paint()
        eraser!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        eraser!!.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

        if (w != oldw || h != oldh) {
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cv = Canvas(bm!!)
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {

        var w = width
        var h = height
        var radius = 0f

        if (w > h)
            radius = h / 2f
        else
            radius = w / 2f

        bm!!.eraseColor(Color.TRANSPARENT)
        cv!!.drawColor(canvassColor)
        cv!!.drawCircle(w / 2f, h / 2f, radius, eraser!!)
        canvas.drawBitmap(bm!!, 0f, 0f, null)
        super.onDraw(canvas)
    }
}