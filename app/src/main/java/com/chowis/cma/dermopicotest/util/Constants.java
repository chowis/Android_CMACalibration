package com.chowis.cma.dermopicotest.util;

import android.os.Environment;

import java.io.File;

public interface Constants {
    String PROJECT_NAME = "CMACamera";
    String DATABASE_NAME = "cma.db";
    String MENU_HAIR = "menu_hair";
    String MENU_SKIN = "menu_skin";
    String MENU = "menu";
    String PICO_PATH = Environment.getExternalStorageDirectory() + File.separator + PROJECT_NAME;
    String IMAGE_PATH = PICO_PATH + File.separator + "images";
    String IMAGE_TEMP_PATH = IMAGE_PATH + File.separator + "temp";


}