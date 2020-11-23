package com.chowis.cma.dermopicotest.util

import android.graphics.*
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.SizeSelector
import com.otaliastudios.cameraview.size.SizeSelectors
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


object CameraUtil {

    fun getOptimalPictureSize(width: Int, height: Int, ratio: AspectRatio): SizeSelector {

        val width = SizeSelectors.minWidth(width)
        val height = SizeSelectors.minHeight(height)
        val dimensions =
            SizeSelectors.and(width, height) // Matches sizes bigger than width x height.
        val ratio = SizeSelectors.aspectRatio(ratio, 0f) // Matches ratio sizes.

        return SizeSelectors.or(
            SizeSelectors.and(ratio, dimensions), // Try to match both constraints
            ratio, // If none is found, at least try to match the aspect ratio
            SizeSelectors.biggest() // If none is found, take the biggest
        )
    }

    @JvmStatic
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        Timber.d("start")
        var b = bitmap
        if (degrees != 0 && b != null) {
            val m = Matrix()
            m.setRotate(degrees.toFloat(), b.width.toFloat() / 2, b.height.toFloat() / 2)
            try {
                val b2 = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
                if (b != b2) {
                    b.recycle()
                    b = b2
                }
            } catch (ex: OutOfMemoryError) {

            }
        }
        return b
    }

    @JvmStatic
    fun resizeBitmap(path: String, menu: String) {
        var scaleBitmap: Bitmap? = null

        val srcBmp = BitmapFactory.decodeFile(path)

        var rotatedBitmap: Bitmap? = null
        if (srcBmp.width >= srcBmp.height) {
            rotatedBitmap = rotateBitmap(srcBmp, 90)
        } else {
            rotatedBitmap = rotateBitmap(srcBmp, 0)
        }
        var ratio  = rotatedBitmap.getWidth() / 4
        val calculatedHeight = if (menu == Constants.MENU_HAIR)rotatedBitmap.width else ratio * 3

        val y = if (menu == Constants.MENU_HAIR)rotatedBitmap.height  /2 - rotatedBitmap.width / 2  else (rotatedBitmap.height - calculatedHeight)/2
        scaleBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            0,
            y,
            rotatedBitmap.width,
            calculatedHeight
        )

        val file = File(path)
        try {
            file.createNewFile()
            val os = FileOutputStream(file)
            val cropBitmap = if (menu == Constants.MENU_HAIR) getCircledBitmap(scaleBitmap) else scaleBitmap

            cropBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.close()
            cropBitmap?.recycle()
            Timber.d("end")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun doProcessingOfBitmap(fileName :String) :Bitmap{
        val mBitmapOptions = BitmapFactory.Options()
        mBitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, mBitmapOptions);
        val  srcWidth = mBitmapOptions.outWidth;
//        srcHeight = mBitmapOptions.outHeight;

        mBitmapOptions.inJustDecodeBounds = false;
        mBitmapOptions.inScaled = true
        mBitmapOptions.inSampleSize = 4
        mBitmapOptions.inDensity = srcWidth;
        mBitmapOptions.inTargetDensity =  320 * mBitmapOptions.inSampleSize;

// will load & resize the image to be 1/inSampleSize dimensions
        return BitmapFactory.decodeFile(fileName, mBitmapOptions);
    }

    private fun getCircledBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat(), (bitmap.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun cropRoundBitmap(srcBitmap: Bitmap): Bitmap? {
        // Calculate the circular bitmap width with border

        // Calculate the circular bitmap width with border
        val squareBitmapWidth: Int = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight())
        // Initialize a new instance of Bitmap
        // Initialize a new instance of Bitmap
        val dstBitmap = Bitmap.createBitmap(
            squareBitmapWidth,  // Width
            squareBitmapWidth,  // Height
            Bitmap.Config.ARGB_8888 // Config
        )
        val canvas = Canvas(dstBitmap)
        // Initialize a new Paint instance
        // Initialize a new Paint instance
        val paint = Paint()
        paint.isAntiAlias = true
        val rect = Rect(0, 0, squareBitmapWidth, squareBitmapWidth)
        val rectF = RectF(rect)
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Calculate the left and top of copied bitmap
        // Calculate the left and top of copied bitmap
        val left: Float = ((squareBitmapWidth - srcBitmap.getWidth()) / 2).toFloat()
        val top: Float = ((squareBitmapWidth - srcBitmap.getHeight()) / 2).toFloat()
        canvas.drawBitmap(srcBitmap, left, top, paint)
        // Free the native object associated with this bitmap.
        // Free the native object associated with this bitmap.
        srcBitmap.recycle()
        // Return the circular bitmap
//        val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
//        roundedBitmapDrawable.cornerRadius = 50.0f
//        roundedBitmapDrawable.setAntiAlias(true)
//        roundedBitmapDrawable.cornerRadius
        return  dstBitmap
    }

}