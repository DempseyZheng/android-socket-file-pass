package com.accvmedia.mysocket.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dempseyZheng on 2016/12/23
 */

public class WifiUtils {
    public static final int WIFICIPHER_NOPASS = 1;
    public static final int WIFICIPHER_WEP    = 2;
    public static final int WIFICIPHER_WPA    = 3;
    private static final String TAG           = "WifiUtils";
    // 定义WifiManager对象
    private WifiManager             mWifiManager;
    // 定义WifiInfo对象
    private WifiInfo                mWifiInfo;
    // 扫描出的网络连接列表
    private List<ScanResult>        mWifiList;
    // 网络连接列表
    private List<WifiConfiguration> mWifiConfiguration;
    // 定义一个WifiLock
    WifiManager.WifiLock mWifiLock;
    private static WifiUtils wifiUtils = null;

    static {
        wifiUtils = new WifiUtils();
    }

    public static WifiUtils getInstance() {
        return wifiUtils;

    }

    // 构造器
    public void init(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
    }

    private WifiUtils() {
    }


    // 打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    // 断开当前网络
    public void disconnectWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.disconnect();
        }
    }

    // 关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 锁定WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                                   true);
    }

    public void startScan() {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder.append("Index_" + Integer.valueOf(i + 1)
                                                   .toString()
                                         + ":");
            // 将ScanResult信息转换成一个字符串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到MAC地址
    public String getMacAddress() {
        if (mWifiManager!=null)
            mWifiInfo = mWifiManager.getConnectionInfo();
        return (mWifiInfo == null)
               ? "NULL"
               : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID
    public String getBSSID() {
        return (mWifiInfo == null)
               ? "NULL"
               : mWifiInfo.getBSSID();
    }

    // 得到IP地址
    public int getIPAddress() {
        if (mWifiManager!=null)
        mWifiInfo = mWifiManager.getConnectionInfo();
        return (mWifiInfo == null)
               ? 0
               : mWifiInfo.getIpAddress();
    }
    private String intToIp(int i) {

        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    public String getParsedIp() {
        return intToIp(getIPAddress());
    }
    // 得到连接的ID
    public int getNetworkId() {
        return (mWifiInfo == null)
               ? 0
               : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包
    public String getWifiInfo() {
        return (mWifiInfo == null)
               ? "NULL"
               : mWifiInfo.toString();
    }

    // 添加一个网络并连接
    public void addNetwork(WifiConfiguration wcg) {
        int     wcgID = mWifiManager.addNetwork(wcg);
        boolean b     = mWifiManager.enableNetwork(wcgID, true);
        System.out.println("netId--" + wcgID);
        System.out.println("是否成功--" + b);
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    public WifiConfiguration CreateWifiInfo(String SSID, String Password,
            int Type)
    {
        if (Password.length()<8){
            throw new RuntimeException("password length must not be less than 8");
        }
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
//        config.SSID = "\"" + SSID + "\"";
        config.SSID =  SSID ;

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == WIFICIPHER_NOPASS) // WIFICIPHER_NOPASS
        {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        if (Type == WIFICIPHER_WEP) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
//            config.wepKeys[0] = "\"" + Password + "\"";
            config.wepKeys[0] =  Password ;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WIFICIPHER_WPA) // WIFICIPHER_WPA
        {
//            config.preSharedKey = "\"" + Password + "\"";
            config.preSharedKey =  Password ;
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        if (existingConfigs!=null&&existingConfigs.size()>0) {
            for (WifiConfiguration existingConfig : existingConfigs) {
//                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                if (existingConfig.SSID.equals( SSID )) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

private boolean setWifiApEnabled(String SSID,String pwd,boolean enable){
    closeWifi();
    WifiConfiguration apConfig = CreateWifiInfo(SSID, pwd,WIFICIPHER_WPA);
    //通过反射调用设置热点
    Method method = null;
    try {
        method = mWifiManager.getClass().getMethod(
                "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);

        Log.e(TAG, "setWifiAp: "+"连接账号："+apConfig.SSID+",密码是："+apConfig.preSharedKey);//提示信息接收方要连接的热点账号和密码

    return (Boolean) method.invoke(mWifiManager, apConfig, enable);
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    public void modifyWifiAp(String SSID,String pwd){
        setWifiApEnabled(SSID,pwd,false);
        setWifiApEnabled(SSID,pwd,true);

    }

    /**
     * 获取局域网的广播地址
     * @param context
     * @return
     * @throws UnknownHostException
     */
    public  InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        DhcpInfo    dhcp = mWifiManager.getDhcpInfo();
        if(dhcp==null) {
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    /**
     * 获取路由器MAC地址
     * @return 返回MAC地址
     */
    public static String getRouterMac(String ip){
        String macAddress = "";
            macAddress = getMacInLinux(ip).trim();
        return macAddress;
    }
    /**
     * @param ip 目标ip
     * @return   Mac Address
     *
     */
    public static String getMacInLinux(final String ip){
        String result = "";
        String[] cmd = {
                "/bin/sh",
                "-c",
                "ping " +  ip + " -c 2 && arp -a"
        };
        String cmdResult = callCmd(cmd);
        result = filterMacAddress(ip,cmdResult,":");

        return result;
    }
    /**
     *
     * @param ip  目标ip,一般在局域网内
     * @param sourceString 命令处理的结果字符串
     * @param macSeparator mac分隔符号
     * @return  mac地址，用上面的分隔符号表示
     */
    public static String filterMacAddress(final String ip, final String sourceString,final String macSeparator) {
        String  result  = "";
        String  regExp  = "((([0-9,A-F,a-f]{1,2}" + macSeparator + "){1,5})[0-9,A-F,a-f]{1,2})";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(sourceString);
        while(matcher.find()){
            result = matcher.group(1);
            if(sourceString.indexOf(ip) <= sourceString.lastIndexOf(matcher.group(1))) {
                break;  //如果有多个IP,只匹配本IP对应的Mac.
            }
        }

        return result;
    }
    public static String callCmd(String[] cmd) {
        String result = "";
        String line = "";
        try {
            Process           proc = Runtime.getRuntime().exec(cmd);
            InputStreamReader is   = new InputStreamReader(proc.getInputStream());
            BufferedReader    br   = new BufferedReader (is);
            while ((line = br.readLine ()) != null) {
                result += line;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

