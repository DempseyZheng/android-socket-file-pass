package com.accvmedia.mysocket.bean;

import java.util.List;

/**
 * Created by dempseyZheng on 2017/3/29
 */
public class BroadcastBean {

    /**
     * hostIp : ip
     * files : [{"name":"txt","length":"kb"},{"name":"zip","length":"kb"}]
     */

    public String          hostIp;
    public int type;
    /**
     * name : txt
     * length : kb
     */

    public List<FilesBean> files;

   public static final int TYPE_FILE=10001;
   public static final int TYPE_CLEAR=10002;
    public static class FilesBean {
        public String name;
        public long length;

        public FilesBean(String name, long length) {
            this.name = name;
            this.length = length;
        }


    }
}
