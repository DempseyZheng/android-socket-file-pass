package com.accvmedia.mysocket.jobqueue;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.accvmedia.mysocket.Constant;
import com.accvmedia.mysocket.bean.SocketBean;
import com.accvmedia.mysocket.socket.SocketManager;
import com.accvmedia.mysocket.util.DebugLogger;
import com.accvmedia.mysocket.util.WifiUtils;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

/**
 * Created by dempseyZheng on 2017/3/31
 */
public class SocketJob
        extends Job {
    private           int          offset;
    private       Socket       socket;
    private       String       mHostIp;
    private       int          mPort;
    private       Handler      mHandler;
    private       FileCallBack callBack;
    private       String       fileName;
    private String mPath="";

    public interface FileCallBack {
        void onProgress(int progress);

        void onStartReceive(String filename, int file_len);

    }


    public SocketJob(String hostIp, int port,int offset,String fileName, Handler handler,
            FileCallBack callBack) {
        this(hostIp, port,offset,fileName);
        mHandler = handler;
        this.callBack = callBack;

    }
    public SocketJob(String hostIp, int port,int offset,String fileName) {
        super(new Params(Constant.PRIORITY).addTags(fileName).groupBy(Constant.GROUP_ID).requireNetwork());
        mHostIp = hostIp;
        mPort = port;
        this.fileName=fileName;
        this.offset = offset;
    }





    @Override
    public void onAdded() {
        
    }

    @Override
    public void onRun()
            throws Throwable
    {
        try {
            socket = new Socket(mHostIp, mPort);
            // socket.setSoTimeout(30000);
            socket.setSoTimeout(Constant.SoTimeout);
            if (mHandler != null)
                Message.obtain(mHandler, SocketManager.C_CONNECTED, "连接成功")
                       .sendToTarget();

            if (socket.isConnected()) {
                if (!socket.isInputShutdown()) {
                    //					String sendMsg = WifiUtils.getInstance().getMacAddress()
                    //							+ "==" + offset;

                    SocketBean bean    =new SocketBean(WifiUtils.getInstance().getMacAddress(), offset, fileName);
                    String     sendMsg =SocketManager.getInstance().parseJson(bean);

                    SocketManager.getInstance().writeByte(sendMsg.getBytes(),
                                                          socket.getOutputStream());

                    InputStream inputStream = socket.getInputStream();

                    readAndSave(inputStream);

                }
            }

//        } catch (Exception ex) {
//            // Constant.isReceiving=false;
//            if (mHandler != null) {
//                Message.obtain(mHandler, SocketManager.C_ERROR, ex)
//                       .sendToTarget();
//
//            }
//            ex.printStackTrace();
        }
            finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        DebugLogger.e(cancelReason+"",throwable);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable,
            int runCount,
            int maxRunCount)
    {
        if (throwable instanceof SocketTimeoutException) {
            DebugLogger.e("retry");
            offset=getOffset(new File(mPath + ".log"));
            return RetryConstraint.RETRY;
        }
        return RetryConstraint.CANCEL;
    }

    private int getOffset(File file) {
        int position = 0;
        if (file.exists()) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            position = Integer.valueOf(properties.getProperty("length"));// 读取断点的位置
        }
        return position;
    }
    // 从流中读取内容并保存
    private void readAndSave(InputStream is) throws IOException {
        Constant.isReceiving = true;
        String filename = SocketManager.getInstance().readByte(is);
        int file_len = SocketManager.getInstance().readInteger(is);
        DebugLogger.i("接收文件：" + filename + "，长度：" + file_len);
        // String receivePath = Constant.RECEIVE_FILE_DIR+"/"+filename;
        // if (isFileReceived(receivePath,file_len)){
        // Message.obtain(mHandler,5,"文件已接收过,路径为: "+receivePath).sendToTarget();
        // socket.close();
        // return;
        // }
        if (callBack != null) {
            callBack.onStartReceive(filename, file_len);
        }

        int fileSize = file_len / 1024;
        if (fileSize == 0) {
            fileSize = 1;
        }
        String startReceive = "开始接收文件,文件名: " + filename + "文件大小: " + fileSize
                + "kb";
        Message.obtain(mHandler, SocketManager.C_MESSAGE, startReceive)
               .sendToTarget();

        File dir = new File(Constant.RECEIVE_FILE_DIR);
        if (!dir.exists())
            dir.mkdir();
        readAndSave0(is, Constant.RECEIVE_FILE_DIR + "/" + filename, file_len);
        String msg = "文件保存成功（" + file_len / 1024 + "kb）。";

        DebugLogger.e(msg);
        Constant.isReceiving = false;

    }

    private void readAndSave0(InputStream is, String path, int file_len)
            throws IOException {

//        Constant.RECEIVE_FILE_PATH = path;
        mPath= path;

        // FileOutputStream os = getFileOS(path);
        // readAndWrite(is, os, file_len);
        // os.close();
        File file = new File(path);

        RandomAccessFile raf = getRandomAccessFile(file);

        readAndWrite(is, raf, file_len, file);
        if (mHandler != null)
            Message.obtain(mHandler, SocketManager.C_RECEIVE_FINISHED, path)
                   .sendToTarget();
    }

    private RandomAccessFile getRandomAccessFile(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        if (!file.exists()) {
            file.createNewFile();
        } else {
            raf.seek(file.length());
        }
        return raf;
    }

    // 创建文件并返回输出流
    private FileOutputStream getFileOS(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }

        return new FileOutputStream(file);
    }

    // 边读边写，直到读取 size 个字节
    private void readAndWrite(InputStream is, FileOutputStream os, int size)
            throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(os, 1024 * 200);

        BufferedInputStream bis    = new BufferedInputStream(is, 1024 * 200);
        byte[]              buffer = new byte[4096];
        int                 count  = 0;
        while (count < size) {
            // int n = is.read(buffer);
            // // 这里没有考虑 n = -1 的情况
            // os.write(buffer, 0, n);
            int n = bis.read(buffer);
            bos.write(buffer, 0, n);
            count += n;
            if (callBack != null)
                callBack.onProgress(count);
        }
        bos.flush();
    }

    private void readAndWrite(InputStream is, RandomAccessFile raf, int size,
            File file) throws IOException {

        BufferedInputStream bis = new BufferedInputStream(is, 1024 * 200);
        byte[] buffer = new byte[4096];
        // int count = 0;
        int count = offset;

        Properties properties = new Properties();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(
                file.getParent(), file.getName() + ".log"), false);

        while (count < size) {
            // int n = is.read(buffer);
            // // 这里没有考虑 n = -1 的情况
            // os.write(buffer, 0, n);
            int n = bis.read(buffer);
            raf.write(buffer, 0, n);
            count += n;
            properties.put("length", String.valueOf(count));

            properties.store(fileOutputStream, null);// 实时记录文件的最后保存位置

            if (callBack != null)
                callBack.onProgress(count);
        }
    }
}
