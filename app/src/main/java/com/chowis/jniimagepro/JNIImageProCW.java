package com.chowis.jniimagepro;

public class JNIImageProCW
{

    static
    {
        System.loadLibrary("JNIImageProCWCore");
    }

    public native String getVersionJni();
    public native String getMakeDateJni();

    public native double CMACalibJni(String sInputPath, String sOutputPath);

}
