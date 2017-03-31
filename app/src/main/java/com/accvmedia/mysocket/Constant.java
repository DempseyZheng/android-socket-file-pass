package com.accvmedia.mysocket;

import android.os.Environment;

/**
 * Created by dempseyZheng on 2017/3/17
 */
public class Constant {
    public static final String RECEIVE_FILE_DIR  = Environment.getExternalStorageDirectory()
                                                             .getAbsolutePath()+"/distribute";
    public static final long RECEIVE_FILE_LENGTH = 0;
    public static final String SEND_DIR          = Environment.getExternalStorageDirectory()
                                                              .getAbsolutePath() + "/send";
    public static final String GROUP_ID          = "SOCKET_GROUP";
    public static final int PRIORITY             = 999;
    public static  String RECEIVE_FILE_PATH      ="";
    public static String       HOST_IP           ="192.168.1.173";
    public static  int         PORT              =10086;
    public static String       multicastHost     ="224.0.0.1";
    public static int          multicastPort     =10010;
    public static boolean isReceiving            =false;
    public static boolean isServer               =false;
    public static int SoTimeout=5*1000;//socket连接超时时间5秒
}
