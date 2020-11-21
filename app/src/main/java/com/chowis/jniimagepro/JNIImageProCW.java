package com.chowis.jniimagepro;


public class JNIImageProCW
{

    static
    {
        System.loadLibrary("JNIImageProCWCore");
    }

    public native String getVersionJni();
    public native String getMakeDateJni();

    public native double appTestJni(String sInputPath, String sOutputPath);
    public native double cropCMAJni(String sInputPath, String sOutputPath);


}