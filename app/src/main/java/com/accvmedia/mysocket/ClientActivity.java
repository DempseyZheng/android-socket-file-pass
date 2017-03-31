package com.accvmedia.mysocket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.accvmedia.mysocket.bean.BroadcastBean;
import com.accvmedia.mysocket.jobqueue.SocketJob;
import com.accvmedia.mysocket.socket.SocketManager;
import com.accvmedia.mysocket.util.FileUtils;
import com.accvmedia.mysocket.util.WifiUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by dempseyZheng on 2017/3/14
 */
public class ClientActivity extends AppCompatActivity
		implements
			View.OnClickListener,
			SocketManager.OnClientListener {

	private static final String TAG = "ClientActivity";
	private TextView tv_msg = null;
	private Button btn_send = null;
	// private Button btn_login = null;
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private String content = "";
	private EditText EditText01;
	private Button Button02;
	private EditText et_host_ip;
	private Button btn_connect;
	private ProgressBar progressBar;
	private String path = "";
	private EditText edt_error_msg;
	private String fileName;
	private boolean isRunning = false;
	private String mPath;
	private long mFileLen;
	private InetAddress mReceiveAddress;
	private ListView act_client_lv;
	List<BroadcastBean.FilesBean> mFileList = new ArrayList<>();
	private EditText et_file_name;
	private ListView act_client_list_view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_new_client);
		initView();
		getSupportActionBar().setTitle(
				"\t\t" + WifiUtils.getInstance().getParsedIp());
		SocketManager.getInstance().setOnClientListener(this);
		// receiveBroadcast();
		act_client_lv.setAdapter(mBaseAdapter);
		act_client_lv
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						BroadcastBean.FilesBean filesBean = mFileList
								.get(position);
						mFileLen = filesBean.length;
						mPath = Constant.RECEIVE_FILE_DIR + "/"
								+ filesBean.name;
						int offset = getOffset(new File(mPath + ".log"));
						SocketManager.getInstance().newClient(mHostIp,
								Constant.PORT, mPath, mFileLen, offset,
								new SocketJob.FileCallBack() {
									@Override
									public void onProgress(int progress) {
										progressBar.setProgress(progress);
									}

									@Override
									public void onStartReceive(
											final String filename,
											final int file_len) {

										progressBar.setMax(file_len);
									}
								});
					}
				});
	}

	private BaseAdapter mBaseAdapter = new BaseAdapter() {
		class ViewHolder {
			public View rootView;
			public TextView client_tv_name;
			public TextView client_tv_len;

			public ViewHolder(View rootView) {
				this.rootView = rootView;
				this.client_tv_name = (TextView) rootView
						.findViewById(R.id.client_tv_name);
				this.client_tv_len = (TextView) rootView
						.findViewById(R.id.client_tv_len);
			}

		}

		@Override
		public int getCount() {
			if (mFileList != null) {
				return mFileList.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return mFileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = View.inflate(ClientActivity.this,
						R.layout.client_item_list, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			// 设置数据
			BroadcastBean.FilesBean filesBean = mFileList.get(position);
			holder.client_tv_name.setText(filesBean.name);
			holder.client_tv_len.setText(filesBean.length + "字节");
			return convertView;
		}
	};
	private String mHostIp;

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

	private boolean isFileReceived(String path, long fileLen) {
		File file = new File(path);

		if (file.exists() && file.length() == fileLen) {
			return true;
		}
		return false;
	}

	private void initView() {
		et_host_ip = (EditText) findViewById(R.id.et_host_ip);
		btn_connect = (Button) findViewById(R.id.btn_connect);

		btn_connect.setOnClickListener(this);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		edt_error_msg = (EditText) findViewById(R.id.edt_error_msg);
		edt_error_msg.setOnClickListener(this);
		act_client_lv = (ListView) findViewById(R.id.act_client_list_view);
		et_file_name = (EditText) findViewById(R.id.et_file_name);
		act_client_list_view = (ListView) findViewById(R.id.act_client_list_view);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

			case R.id.btn_connect :
				connect(et_host_ip.getText().toString().trim(),
						Constant.RECEIVE_FILE_DIR + "/"
								+ et_file_name.getText().toString().trim(), 0);
				break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	private void connect(String hostIp, String path, long len) {
		// 判断文件是否已经接收完成过

		Constant.HOST_IP = hostIp;

		SocketManager.getInstance().newClient(hostIp, 9999, path, len,
				getOffset(new File(mPath + ".log")),
				new SocketJob.FileCallBack() {

					@Override
					public void onProgress(int progress) {
						progressBar.setProgress(progress);
					}

					@Override
					public void onStartReceive(final String filename,
							final int file_len) {

						progressBar.setMax(file_len);
					}

				});
	}

	@Override
	public void onConnected() {
		String msg = "连接成功";
		// 连接成功
		Toast.makeText(ClientActivity.this, msg, Toast.LENGTH_SHORT).show();
		String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(System.currentTimeMillis());
		edt_error_msg.append("\n" + format + "-->" + msg);
	}

	@Override
	public void onReceiveFinished(String savepath) {
		Toast.makeText(ClientActivity.this, "文件接收完成", Toast.LENGTH_SHORT)
				.show();
		String format = new SimpleDateFormat("yyyy-MM-dd H:m:s").format(System
				.currentTimeMillis());
		edt_error_msg.append("\n" + format + "-->" + "文件接收完成");
//		int index = savepath.lastIndexOf(".");
//		if (index == -1) {
//			return;
//		}
//		String substring = savepath.substring(index);
//
//		if (substring.equals(".apk")) {
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			intent.setDataAndType(Uri.fromFile(new File(savepath)),
//					"application/vnd.android.package-archive");
//			startActivity(intent);
//		}
	}

	@Override
	public void onError(Exception ex) {
		if (ex instanceof ConnectException) {
			Toast.makeText(ClientActivity.this, "连接异常", Toast.LENGTH_SHORT)
					.show();

		} else if (ex instanceof SocketTimeoutException) {
			edt_error_msg.append("\n 连接超时,重连服务器...");
//			connect(mHostIp, mPath, mFileLen);

		} else {
			Toast.makeText(ClientActivity.this, "出现异常", Toast.LENGTH_SHORT)
					.show();

		}
		String format = new SimpleDateFormat("yyyy-MM-dd H:m:s").format(System
				.currentTimeMillis());
		edt_error_msg.append("\n" + format + "-->"
				+ Log.getStackTraceString(ex));
	}

	@Override
	public void onMessage(String msg) {
		String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(System.currentTimeMillis());
		edt_error_msg.append("\n" + format + "-->" + msg);
	}

	@Override
	public void onBroadcastMsg(BroadcastBean broadcastMsg) {
		switch (broadcastMsg.type) {
			case BroadcastBean.TYPE_CLEAR :
				boolean b = FileUtils
						.deleteFilesInDir(Constant.RECEIVE_FILE_DIR);
				edt_error_msg.append("\n" + "清空文件夹: "
						+ Constant.RECEIVE_FILE_DIR + ",结果: " + b);
				break;
			case BroadcastBean.TYPE_FILE :
				mFileList.clear();
				String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(System.currentTimeMillis());
				edt_error_msg.append("\n" + format + "-->" + "接收到服务器ip地址: "
						+ broadcastMsg.hostIp);
				for (int i = 0; i < broadcastMsg.files.size(); i++) {
					edt_error_msg.append("\n" + "接收的文件名: "
							+ broadcastMsg.files.get(i).name + "," + "文件大小:"
							+ broadcastMsg.files.get(i).length + "字节");
				}
				mFileList = broadcastMsg.files;
				mBaseAdapter.notifyDataSetChanged();
				mHostIp = broadcastMsg.hostIp;

				for (int i = 0; i < mFileList.size(); i++) {

					BroadcastBean.FilesBean filesBean = mFileList.get(i);
					long fileLen = filesBean.length;
					String path = Constant.RECEIVE_FILE_DIR + "/"
							+ filesBean.name;
					int offset = getOffset(new File(path + ".log"));

					SocketManager.getInstance().newClient(mHostIp,
							Constant.PORT, path, fileLen, offset,
							new SocketJob.FileCallBack() {

								@Override
								public void onProgress(int progress) {
									progressBar.setProgress(progress);
								}

								@Override
								public void onStartReceive(
										final String filename,
										final int file_len) {

									progressBar.setMax(file_len);
								}

							});
				}

				// mFileLen = broadcastMsg.files.get(0).length;
				// mPath = Constant.RECEIVE_FILE_DIR + "/"
				// + broadcastMsg.files.get(0).name;
				// int offset = getOffset(new File(mPath + ".log"));
				// SocketManager.getInstance()
				// .newClient(mHostIp, Constant.PORT,
				// mPath, mFileLen, offset,
				// new ClientThread.FileCallBack() {
				// @Override
				// public void onProgress(int progress) {
				// progressBar.setProgress(progress);
				// }
				//
				// @Override
				// public void onStartReceive(final String filename,
				// final int file_len)
				// {
				//
				// progressBar.setMax(file_len);
				// }
				// });

				break;
			default :

				break;
		}

		// if (broadcastMsg.contains("clear")) {
		// boolean b = FileUtils.deleteFilesInDir(Constant.RECEIVE_FILE_DIR);
		// edt_error_msg.append("\n" + "清空文件夹: " + Constant.RECEIVE_FILE_DIR
		// + ",结果: " + b);
		// } else {
		// String[] split = broadcastMsg.split("==");
		// if (split.length == 3) {
		// String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		// .format(System.currentTimeMillis());
		// edt_error_msg.append("\n" + format + "-->" + "接收到服务器ip地址: "
		// + split[0] + ",接收的文件名: " + split[1] + "," + "文件大小:"
		// + split[2] + "字节");
		// mHostIp = split[0];
		// mFileLen = Long.parseLong(split[2]);
		// mPath = Constant.RECEIVE_FILE_DIR + "/" + split[1];

		// }
		// }
	}

}
