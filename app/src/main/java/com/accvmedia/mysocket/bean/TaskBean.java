package com.accvmedia.mysocket.bean;

/**
 * Created by dempseyZheng on 2017/3/21
 */
public class TaskBean {
    public String ip;
    public String fileName;
    public int fileLen;
    public int progress;

    public TaskBean(String ip, String fileName, String wifiMac, int fileLen) {
        this.ip = ip;
        this.fileName = fileName;
        this.wifiMac = wifiMac;
        this.fileLen = fileLen;
    }

    public String wifiMac;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskBean taskBean = (TaskBean) o;

        if (fileLen != taskBean.fileLen) {
            return false;
        }
        if (ip != null
            ? !ip.equals(taskBean.ip)
            : taskBean.ip != null)
        {
            return false;
        }
        if (fileName != null
            ? !fileName.equals(taskBean.fileName)
            : taskBean.fileName != null)
        {
            return false;
        }
        return wifiMac != null
               ? wifiMac.equals(taskBean.wifiMac)
               : taskBean.wifiMac == null;

    }

    @Override
    public int hashCode() {
        int result = ip != null
                     ? ip.hashCode()
                     : 0;
        result = 31 * result + (fileName != null
                                ? fileName.hashCode()
                                : 0);
        result = 31 * result + fileLen;
        result = 31 * result + (wifiMac != null
                                ? wifiMac.hashCode()
                                : 0);
        return result;
    }
}
