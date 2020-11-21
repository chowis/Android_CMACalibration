package com.chowis.cma.dermopicotest.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Calibrate(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var image_id: String,
    var imagePath: String,
    var resultPath: String,
    var time: String,
    var date: String,
    var type: String,
    var sizeFactor: String
)
