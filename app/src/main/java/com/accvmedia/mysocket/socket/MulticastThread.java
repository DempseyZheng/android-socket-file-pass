package com.accvmedia.mysocket.socket;

import com.accvmedia.mysocket.Constant;
import com.accvmedia.mysocket.util.DebugLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by dempseyZheng on 2017/3/27
 */
public class MulticastThread
        extends Thread {
    private byte[] mSendMSG;

    public MulticastThread(byte[] sendMSG) {

        mSendMSG = sendMSG;
    }

    @Override
    public void run() {
        try {
            InetAddress broadAddress = InetAddress
                    .getByName(Constant.multicastHost);
            if (!broadAddress.isMulticastAddress()) {// 测试是否为多播地址
//                throw new RuntimeException("请使用多播地址");
              DebugLogger.e("请使用多播地址");
            }
            MulticastSocket multiSocket = new MulticastSocket();
            multiSocket.setTimeToLive(4);


            DatagramPacket dp = new DatagramPacket(mSendMSG,
                                                   mSendMSG.length, broadAddress,
                                                   Constant.multicastPort);

            multiSocket.send(dp);

            multiSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
