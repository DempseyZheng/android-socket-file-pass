package com.accvmedia.mysocket.socket;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.accvmedia.mysocket.Constant;
import com.accvmedia.mysocket.bean.SocketBean;
import com.accvmedia.mysocket.bean.TaskBean;
import com.accvmedia.mysocket.util.DebugLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by dempseyZheng on 2017/3/15
 */
public class ServerRunnable implements Runnable {
	private Socket            socket;
	private Handler           mHandler;
	private ArrayList<String> mFileNames;
	private String            mPath;
	private String            ip;
	private String            mWifiMac;
	private ProgressCallBack  mCallBack;
	private TaskBean          mTaskBean;
	private int               offset;
	private String            fileName;

	public interface ProgressCallBack {
		void onProgress(TaskBean taskBean, int progress);
	}
	public ServerRunnable(Socket socket, Handler handler, String path,
			ProgressCallBack callBack) {
		mCallBack = callBack;
		this.socket = socket;
		mHandler = handler;
		mPath = path;
		this.ip = socket.getInetAddress().getHostAddress();
	}
	public ServerRunnable(Socket socket, Handler handler,
			ArrayList<String> fileNames,	ProgressCallBack callBack) {
		this.socket = socket;
		mHandler = handler;
		mFileNames = fileNames;
		this.ip = socket.getInetAddress().getHostAddress();
		mCallBack = callBack;
	}


	@Override
	public void run() {
		try {
			// if (mFileNames!=null){
			// sendFile(socket.getOutputStream(),mFileNames);
			// }else{
			//
			// sendFile(socket.getOutputStream(), mPath);
			// }
			// 拿到连接的mac地址
			InputStream inputStream = socket.getInputStream();
			String recMsg = SocketManager.getInstance().readByte(inputStream);



//			String[] split = recMsg.split("==");
//			if (split.length > 0) {
//				mWifiMac = split[0];
//				offset = Integer.valueOf(split[1]);
//			}

			//解析客户端数据
			SocketBean socketBean=SocketManager.getInstance().fromJson(recMsg);
			mWifiMac=socketBean.wifiMac;
			offset=socketBean.offset;
			fileName = socketBean.fileName;
			DebugLogger.e(mWifiMac+"=="+offset+"=="+fileName);
			File file=new File(Constant.SEND_DIR);

			if (!file.exists())file.mkdir();

			String path = Constant.SEND_DIR+"/"+fileName;

			if (mHandler != null)
				Message.obtain(mHandler, SocketManager.S_MESSAGE,
							   ip + "的wifiMac: " + mWifiMac).sendToTarget();
			sendFile(socket.getOutputStream(), path);

		} catch (Exception e) {
			if (mHandler != null)
				Message.obtain(mHandler, SocketManager.S_ERROR, e)
					   .sendToTarget();
			e.printStackTrace();
		}
	}




	public void sendFile(OutputStream os, String filepath) throws IOException {

		File file = new File(filepath);
		// FileInputStream is = new FileInputStream(filepath);
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		try {
			int length = (int) file.length();
			String msg = "给" + ip + "发送文件：" + file.getName() + "，长度：" + length;
			DebugLogger.i(msg);
			mTaskBean = new TaskBean(ip, file.getName(), mWifiMac, length);

			Message.obtain(mHandler, SocketManager.S_START_SEND, mTaskBean)
					.sendToTarget();
			// 发送文件名和文件内容
			// writeFileName(file, os);
			SocketManager.getInstance()
					.writeByte(file.getName().getBytes(), os);

			// writeFileContent(is, os, length);
			writeFileContent(raf, os, length);
		} finally {
			os.close();
			raf.close();
		}
	}

	// public void sendFile(OutputStream os, ArrayList<String> filepaths)
	// throws IOException {
	// for (int i = 0; i < filepaths.size(); i++) {
	// String filePath = filepaths.get(i);
	//
	// File file = new File(filePath);
	// FileInputStream is = new FileInputStream(filePath);
	//
	// try {
	// int length = (int) file.length();
	// DebugLogger.i("给" + ip + "发送文件：" + file.getName() + "，长度："
	// + length);
	//
	// // 发送文件名和文件内容
	// writeFileName(file, os);
	// writeFileContent(is, os, length);
	// } finally {
	// os.close();
	// is.close();
	// }
	//
	// }
	//
	// }

	// 输出文件内容
	// private void writeFileContent(InputStream is, OutputStream os, int
	// length)
	// throws IOException {
	// // 输出文件长度
	// os.write(SocketManager.getInstance().i2b(length));
	//
	// BufferedInputStream bis = new BufferedInputStream(is,1024*200);
	// BufferedOutputStream bos = new BufferedOutputStream(os,1024*200);
	//
	// // 输出文件内容
	// byte[] buffer = new byte[4096];
	// int size;
	// long progress = 0;
	// // int buf=0;
	// long startTime = System.currentTimeMillis();
	// // while ((size = is.read(buffer)) != -1) {
	// // os.write(buffer, 0, size);
	// // progress += size;
	// // DebugLogger.d(ip + ": " + progress);
	// // buf+=size;
	// // if (buf>4096){
	// // buf=0;
	// // }
	// // if (mCallBack!=null){
	// // mCallBack.onProgress(mTaskBean, (int) progress);
	// // }
	//
	// // if (mHandler!=null){
	// // mTaskBean.progress= (int) progress;
	// // Message.obtain(mHandler,5,mTaskBean).sendToTarget();
	// // }
	//
	// // }
	// while ((size = bis.read(buffer)) != -1) {
	// bos.write(buffer, 0, size);
	// progress += size;
	// DebugLogger.d(ip + ": " + progress);
	// if (mCallBack != null) {
	// mCallBack.onProgress(mTaskBean, (int) progress);
	// }
	// }
	// bos.flush();
	// long costTime = System.currentTimeMillis() - startTime;
	// int costSecond = (int) (costTime / 1000);
	// if (costSecond == 0) {
	// costSecond = 1;
	// }
	//
	// SystemClock.sleep(500);
	// String result = "给" + ip + "发送文件完成,耗时:" + costSecond + "秒,下载速度:"
	// + length / 1024 / costSecond + "kb/秒";
	// DebugLogger.e(result);
	// if (mHandler != null) {
	// Message.obtain(mHandler, SocketManager.S_MESSAGE, result).sendToTarget();
	// mTaskBean.progress = length;
	// Message.obtain(mHandler, SocketManager.S_SEND_FINISHED,
	// mTaskBean).sendToTarget();
	//
	// }
	// socket.close();
	// }

	private void writeFileContent(RandomAccessFile raf, OutputStream os,
			int length) throws IOException {
		// 输出文件长度
		os.write(SocketManager.getInstance().i2b(length));
		raf.seek(offset);
		DebugLogger.e("文件位置:" + offset);
		BufferedOutputStream bos = new BufferedOutputStream(os, 1024 * 200);

		// 输出文件内容
		byte[] buffer = new byte[4096];
		int size;
		long progress = 0;
		// int buf=0;
		long startTime = System.currentTimeMillis();

		while ((size = raf.read(buffer)) != -1) {
			bos.write(buffer, 0, size);
			progress += size;
			DebugLogger.d(ip + ": " + progress);
			if (mCallBack != null) {
				mCallBack.onProgress(mTaskBean, (int) progress);
			}
		}
		bos.flush();
		long costTime = System.currentTimeMillis() - startTime;
		int costSecond = (int) (costTime / 1000);
		if (costSecond == 0) {
			costSecond = 1;
		}
		int kbLen = length / 1024;
		if (kbLen == 0) {
			kbLen = 1;
		}
		SystemClock.sleep(500);
		String result = "给" + ip + "发送文件完成,耗时:" + costSecond + "秒,下载速度:"
				+ kbLen / costSecond + "kb/秒";
		DebugLogger.e(result);
		if (mHandler != null) {
			Message.obtain(mHandler, SocketManager.S_MESSAGE, result)
					.sendToTarget();
			mTaskBean.progress = length;
			Message.obtain(mHandler, SocketManager.S_SEND_FINISHED, mTaskBean)
					.sendToTarget();

		}
		socket.close();
	}


}
