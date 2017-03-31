package com.accvmedia.mysocket.socket;

import android.os.Handler;
import android.os.Message;

import com.accvmedia.mysocket.Constant;
import com.accvmedia.mysocket.bean.BroadcastBean;
import com.accvmedia.mysocket.bean.SocketBean;
import com.accvmedia.mysocket.bean.TaskBean;
import com.accvmedia.mysocket.jobqueue.SocketJob;
import com.accvmedia.mysocket.jobqueue.SocketJobManager;
import com.accvmedia.mysocket.util.DebugLogger;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dempseyZheng on 2017/3/15
 */
public class SocketManager {
	private static SocketManager mSocketManager = null;
	static {
		mSocketManager = new SocketManager();
	}

	private ServerSocket server;
	private ExecutorService mExecutorService;
	private boolean isServerRunning = false;
	private boolean isReceiveBroadcast = false;
	private String                 mFilePath;
//	private Handler mClientHandler;
	private SocketJob.FileCallBack mCallBack;
	private MulticastSocket        mReceiveMulticast;
	private InetAddress            mReceiveAddress;

	public static final int C_CONNECTED = 1001;
	public static final int C_RECEIVE_FINISHED = 1002;
	public static final int C_ERROR = 1003;
	public static final int C_MESSAGE = 1004;
	public static final int C_BROADCAST_MESSAGE = 1005;

	public static final int S_MESSAGE = 2001;
	public static final int S_ERROR = 2002;
	public static final int S_STARTED = 2003;
	public static final int S_START_SEND = 2004;
	public static final int S_UPDATE_PROGRESS = 2005;
	public static final int S_SEND_FINISHED = 2006;

	private OnClientListener cListener;
	private Gson mGson;
	private ArrayList<String> mFilePaths;

	public void setOnClientListener(OnClientListener cListener) {

		this.cListener = cListener;
	}

	public String parseJson(Object obj) {
		return mGson.toJson(obj);
	}

	public SocketBean fromJson(String recMsg) {

		return mGson.fromJson(recMsg,SocketBean.class);
	}

	public interface OnClientListener {
		void onConnected();
		void onReceiveFinished(String savepath);
		void onError(Exception ex);
		void onMessage(String msg);
//		void onBroadcastMsg(Object obj);
		void onBroadcastMsg(BroadcastBean broadcastBean);
	}

	private OnServerListener sListener;
	public void setOnServerListener(OnServerListener sListener) {

		this.sListener = sListener;
	}
	public interface OnServerListener {
		void onStarted();
		void onStartSend(TaskBean startSend);
		void onError(Exception ex);
		void onMessage(String msg);
		void onSendFinished(TaskBean sendFinished);
		void onUpdateProgress(TaskBean updateProgress);
	}
	public static SocketManager getInstance() {
		return mSocketManager;
	}

	public void init() {
		if (!Constant.isServer)
			receiveBroadcast();
		mGson = new Gson();



	}
	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case C_CONNECTED :
					if (cListener != null) {
						cListener.onConnected();
					}

					break;
				case C_RECEIVE_FINISHED :
					if (cListener != null) {
						cListener.onReceiveFinished((String) msg.obj);
					}

					break;
				case C_ERROR :
					Exception cEx = (Exception) msg.obj;
					if (cListener != null) {
						cListener.onError(cEx);
					}
					SocketManager.getInstance().handleClientEx(cEx);

					break;
				case C_MESSAGE :
					// 显示信息
					String info = (String) msg.obj;
					if (cListener != null) {
						cListener.onMessage(info);
					}

					break;
				case C_BROADCAST_MESSAGE :
					// 收到广播信息

//					String broadMsg = (String) msg.obj;
					BroadcastBean broadMsg = (BroadcastBean) msg.obj;
					if (cListener != null) {
//						cListener.onBroadcastMsg(msg.obj);
						cListener.onBroadcastMsg(broadMsg);
					}
					break;
				case S_STARTED :
					if (sListener != null) {
						sListener.onStarted();
					}
					break;
				case S_START_SEND :
					TaskBean startSend = (TaskBean) msg.obj;
					if (sListener != null) {
						sListener.onStartSend(startSend);
					}
					break;
				case S_UPDATE_PROGRESS :
					TaskBean progress = (TaskBean) msg.obj;
					if (sListener != null) {
						sListener.onUpdateProgress(progress);
					}
					break;
				case S_MESSAGE :
					if (sListener != null) {
						sListener.onMessage((String) msg.obj);
					}
					break;
				case S_SEND_FINISHED :
					TaskBean overBean = (TaskBean) msg.obj;
					if (sListener != null) {
						sListener.onSendFinished(overBean);
					}
					break;
				case S_ERROR :
					Exception sEx = (Exception) msg.obj;
					if (sListener != null) {
						sListener.onError(sEx);
					}
					handleClientEx(sEx);
					break;
				default :

					break;
			}
		}
	};



	/**
	 * 启动socket服务器
	 *
	 * @param filePath
	 *            要发送的文件路径
	 */
	public void startServer(String filePath,
			final ServerRunnable.ProgressCallBack callBack) {
		mFilePath = filePath;
		closeServer();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					server = new ServerSocket(Constant.PORT);
					server.setReuseAddress(true);
					mExecutorService = Executors.newCachedThreadPool();

					Message.obtain(mHandler, S_STARTED, "服务器已开启").sendToTarget();
					isServerRunning = true;
					Socket client = null;
					while (isServerRunning) {
						client = server.accept();
						mExecutorService.execute(new ServerRunnable(client,
								mHandler, mFilePath, callBack));

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}



	/**
	 * 启动socket服务器
	 *
	 * @param filePaths
	 *            要发送的文件路径
	 */
	public void startServer(ArrayList<String> filePaths,
			final ServerRunnable.ProgressCallBack callBack) {
		mFilePaths = filePaths;
		closeServer();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					server = new ServerSocket(Constant.PORT);
					server.setReuseAddress(true);
					mExecutorService = Executors.newCachedThreadPool();

					Message.obtain(mHandler, S_STARTED, "服务器已开启").sendToTarget();
					isServerRunning = true;
					Socket client = null;
					while (isServerRunning) {
						client = server.accept();
						mExecutorService.execute(new ServerRunnable(client,
																	mHandler, mFilePaths, callBack));

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 关闭socket服务器
	 */
	public void closeServer() {
		isServerRunning = false;
		if (server != null) {
			try {
				server.close();
				server = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 创建一个socket客户端
	 *
	 * @param hostIp
	 *            要连接的主机ip地址
	 * @param port
	 *            要连接的端口号
	 * @param path
	 *            接收的文件路径
	 * @param fileLen
	 *            接收的文件字节长度
	 * @param offset
	 *            文件写入位置
	 * @param callBack
	 *            回调
	 */
	public void newClient(String hostIp, int port, String path, long fileLen,
			int offset, SocketJob.FileCallBack callBack) {
		// if (Constant.isReceiving){
		// if (handler!=null)
		// Message.obtain(handler,3," 文件正在接收....不要重复接收").sendToTarget();
		// return;
		// }
		File file = new File(path);

		if (isFileReceived(file, fileLen)) {
			if (mHandler != null)
				Message.obtain(mHandler, C_MESSAGE, " 文件已经接收过,路径为: " + path)
						.sendToTarget();
			return;
		}
		if (isFileReceiving(file, fileLen)) {
			if (mHandler != null)
				Message.obtain(mHandler, C_MESSAGE, " 文件正在接收...").sendToTarget();
			return;
		}
		mCallBack = callBack;
//		Message.obtain(mHandler, C_MESSAGE, "开始连接服务器... ").sendToTarget();
		String fileName=file.getName();
//		new ClientThread(hostIp, port, offset, fileName, mHandler, callBack).start();
		SocketJobManager.getInstance().addJobInBackground(new SocketJob(hostIp,port,offset,fileName,mHandler,callBack));

	}

	private boolean isFileReceiving(File file, long fileLen) {

		if (file.exists() && file.length() != fileLen && Constant.isReceiving) {
			return true;
		}
		return false;
	}

	/**
	 * 创建一个socket客户端
	 *
	 * @param hostIp
	 *            要连接的主机ip地址
	 * @param port
	 *            要连接的端口号
	 */
	public void newClient(String hostIp, int port) {
		// new ClientThread(hostIp, port, mClientHandler, mCallBack).start();
		newClient(hostIp, port, "", 0, 0, mCallBack);
	}

	public void reconnect(String hostIp, int port) {
		if (isFileReceived(new File(Constant.RECEIVE_FILE_PATH),
				Constant.RECEIVE_FILE_LENGTH)) {
			Message.obtain(mHandler, C_MESSAGE,
					" 文件已经接收过,路径为: " + Constant.RECEIVE_FILE_PATH)
					.sendToTarget();
		} else {

			newClient(hostIp, port);
		}
	}

	private boolean isFileReceived(File file, long fileLen) {

		if (file.exists() && file.length() == fileLen) {
			return true;
		}
		return false;
	}

	public void onDestroy() {
		isReceiveBroadcast = false;
		isServerRunning = false;
		if (mReceiveMulticast != null && mReceiveAddress != null) {
			try {
				mReceiveMulticast.leaveGroup(mReceiveAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void receiveBroadcast() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				isReceiveBroadcast = true;
				try {

					while (isReceiveBroadcast) {
						mReceiveAddress = null;

						mReceiveAddress = InetAddress
								.getByName(Constant.multicastHost);

						if (!mReceiveAddress.isMulticastAddress()) {// 测试是否为多播地址

							DebugLogger.e("请使用多播地址");

						}
						mReceiveMulticast = new MulticastSocket(
								Constant.multicastPort);
						mReceiveMulticast.setReuseAddress(true);
						mReceiveMulticast.joinGroup(mReceiveAddress);

						DatagramPacket dp = new DatagramPacket(new byte[1024],
								1024);

						mReceiveMulticast.receive(dp);
						String msg = new String(dp.getData()).trim();
						BroadcastBean broadcastBean = mGson.fromJson(msg, BroadcastBean.class);
						DebugLogger.e(msg);
						Message.obtain(mHandler, C_BROADCAST_MESSAGE, broadcastBean).sendToTarget();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	// 将 int 转成字节
	public byte[] i2b(int i) {
		return new byte[]{(byte) ((i >> 24) & 0xFF), (byte) ((i >> 16) & 0xFF),
				(byte) ((i >> 8) & 0xFF), (byte) (i & 0xFF)};
	}

	// 输出byte长度和内容
	public void writeByte(byte[] bytes, OutputStream os) throws IOException {
		os.write(i2b(bytes.length)); // 输出文件名长度
		os.write(bytes); // 输出文件名
	}

	// 根据byte长度读取内容
	public String readByte(InputStream is) throws IOException {
		int name_len = readInteger(is);
		byte[] result = new byte[name_len];
		is.read(result);
		return new String(result);
	}

	// 读取一个数字
	public int readInteger(InputStream is) throws IOException {
		byte[] bytes = new byte[4];
		is.read(bytes);
		return b2i(bytes);
	}

	// 将字节转成 int。b 长度不得小于 4，且只会取前 4 位。
	public int b2i(byte[] b) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	public void handleClientEx(Exception ex) {
		Constant.isReceiving = false;
		if (ex instanceof ConnectException) {
			DebugLogger.e("连接异常");
		} else if (ex instanceof SocketTimeoutException) {
			DebugLogger.e("连接超时,重连服务器...");

		}
	}
}
