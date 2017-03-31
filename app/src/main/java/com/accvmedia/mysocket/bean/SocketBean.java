package com.accvmedia.mysocket.bean;

/**
 * Created by dempseyZheng on 2017/3/29
 */
public class SocketBean {

    /**
     * wifiMac : mac
     * offset : 0
     * file : txt
     */

    public String wifiMac;
    public int    offset;
    public String fileName;

    public SocketBean(String wifiMac, int offset, String fileName) {
        this.wifiMac = wifiMac;
        this.offset = offset;
        this.fileName = fileName;
    }
}
