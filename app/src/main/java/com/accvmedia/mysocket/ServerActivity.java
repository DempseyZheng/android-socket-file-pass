package com.accvmedia.mysocket;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.accvmedia.mysocket.bean.BroadcastBean;
import com.accvmedia.mysocket.bean.TaskBean;
import com.accvmedia.mysocket.socket.MulticastThread;
import com.accvmedia.mysocket.socket.ServerRunnable;
import com.accvmedia.mysocket.socket.SocketManager;
import com.accvmedia.mysocket.util.DebugLogger;
import com.accvmedia.mysocket.util.WifiUtils;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ServerActivity extends AppCompatActivity
		implements
			ServerRunnable.ProgressCallBack,
			View.OnClickListener,
			SocketManager.OnServerListener {

	private static final int PORT = 10086;
	private static final int FILE_CODE = 1010;
	private ServerSocket server;
	private ExecutorService mExecutorService;
	private ArrayList<Socket> mList = new ArrayList<>();
	public boolean isRunnning = true;
	private EditText et_client_msg;
	private ArrayList<TaskBean> mDataList = new ArrayList<>();
	private ArrayList<HomeAdapter.MyViewHolder> mHolderList = new ArrayList<>();

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String format = "";
			switch (msg.what) {
				case 0 :
					format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(System.currentTimeMillis());
					et_client_msg.append("\n" + format + "-->" + msg.obj);
					break;
				case 1 :
					// mHomeAdapter.notifyDataSetChanged();
					// mRecyclerView.smoothScrollToPosition(mDataList.size()-1);
					break;

				case 2 :
					Exception ex = (Exception) msg.obj;
					if (ex instanceof ConnectException) {
						Toast.makeText(ServerActivity.this, "连接异常",
								Toast.LENGTH_SHORT).show();
					}

					else {
						Toast.makeText(ServerActivity.this, "出现异常",
								Toast.LENGTH_SHORT).show();

					}
					String format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(System.currentTimeMillis());
					et_error_msg.append("\n" + format1 + "-->"
							+ Log.getStackTraceString(ex));
					break;
				case 3 :
					System.out.print("server start ...");

					format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(System.currentTimeMillis());
					et_client_msg.append("\n" + format + "-->"
							+ (String) msg.obj);
					break;
				case 4 :
					DebugLogger.e("开始发送");
					TaskBean taskBean = (TaskBean) msg.obj;
					mDataList.add(taskBean);
					// Message message = Message.obtain();
					// message.what = 1;
					// mHandler.sendMessage(message);
					DebugLogger.e("更新列表");
					// mHomeAdapter.notifyDataSetChanged();
					// mRecyclerView.smoothScrollToPosition(mDataList.size()-1);
					mAdapter.notifyDataSetChanged();
					list_view.smoothScrollToPosition(mDataList.size() - 1);
					break;
				case 5 :
					// TaskBean progress= (TaskBean) msg.obj;
					// DebugLogger.d(progress.ip + ": " + progress.progress);
					// View view = null;
					// for (int i = 0; i < viewList.size(); i++) {
					// if (viewList.get(i).getTag(progress.hashCode()) != null)
					// {
					// view = viewList.get(i);
					// break;
					// }
					// }
					// if (view != null) {
					// ServerAdapter.ViewHolder viewHolder =
					// (ServerAdapter.ViewHolder) view
					// .getTag();
					// viewHolder.item_pb.setProgress(progress.progress);
					// }
					TaskBean progress = (TaskBean) msg.obj;
					// for (int i = 0; i < mHolderList.size(); i++) {
					// HomeAdapter.MyViewHolder myViewHolder =
					// mHolderList.get(i);
					//
					// if
					// (myViewHolder.item_tv_wifi_mac.getText().toString().equals(progress.wifiMac)){
					// myViewHolder.item_pb.setProgress(progress.progress);
					// mHomeAdapter.notifyItemChanged(myViewHolder.item_pb.getId());
					// break;
					//
					// }

					// }
					HomeAdapter.MyViewHolder myViewHolder = mholderMap
							.get(progress);
					if (myViewHolder != null) {
						myViewHolder.item_pb.setProgress(progress.progress);
						mHomeAdapter.notifyItemChanged(myViewHolder.item_pb
								.getId());
					}
					break;
				case 6 :

					TaskBean overBean = (TaskBean) msg.obj;
					// String result = "给" + overBean.ip + "发送文件完成";
					// format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					// .format(System.currentTimeMillis());
					// et_client_msg.append("\n" + format + "-->"
					// + result);
					updateProgress(overBean, overBean.fileLen);

					// HomeAdapter.MyViewHolder overViewHolder =
					// mholderMap.get(overBean);
					// if (overViewHolder!=null) {
					// DebugLogger.e("设置进度"+overBean.progress);
					// overViewHolder.item_pb.setProgress(overBean.progress);
					// mHomeAdapter.notifyItemChanged(overViewHolder.item_pb.getId());
					// }

					break;
				default :

					break;
			}
		}
	};
	private Button btn_clear;
	private Button btn_restart_server;
	private Button btn_send;
	private EditText et_error_msg;
	private String path = "";
//	private ArrayList<String> paths;
	private Button btn_send_broadcast;
	private ListView list_view;
	private ServerAdapter mAdapter;
	private HomeAdapter mHomeAdapter;
	private RecyclerView mRecyclerView;
	private HashMap<TaskBean, HomeAdapter.MyViewHolder> mholderMap = new HashMap<>();
	private Button btn_clear_dir_broadcast;
	private ArrayList<String> mPaths;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		initView();
		getSupportActionBar().setTitle(
				"\t\t" + WifiUtils.getInstance().getParsedIp());
		mAdapter = new ServerAdapter(viewList, mDataList, this);
		list_view.setAdapter(mAdapter);

		// 设置布局管理器
		// mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		// mRecyclerView.setAdapter(mHomeAdapter = new HomeAdapter());
		// 设置Item增加、移除动画
		// mRecyclerView.setItemAnimator(null);

		SocketManager.getInstance().setOnServerListener(this);
	}

	@Override
	public void onStarted() {
		DebugLogger.e("server start ...");

		String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(System.currentTimeMillis());
		et_client_msg.append("\n" + format + "-->" + "server start ...");
	}

	@Override
	public void onStartSend(TaskBean startSend) {
		DebugLogger.e("开始发送");
		mDataList.add(startSend);
		mAdapter.notifyDataSetChanged();
		list_view.smoothScrollToPosition(mDataList.size() - 1);
	}

	@Override
	public void onError(Exception ex) {
		if (ex instanceof ConnectException) {
			Toast.makeText(ServerActivity.this, "连接异常", Toast.LENGTH_SHORT)
					.show();
		}

		else {
			Toast.makeText(ServerActivity.this, "出现异常", Toast.LENGTH_SHORT)
					.show();

		}
		String format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(System.currentTimeMillis());
		et_error_msg.append("\n" + format1 + "-->"
				+ Log.getStackTraceString(ex));
	}

	@Override
	public void onMessage(String msg) {
		String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(System.currentTimeMillis());
		et_client_msg.append("\n" + format + "-->" + msg);
	}

	@Override
	public void onSendFinished(TaskBean sendFinished) {
		updateProgress(sendFinished, sendFinished.fileLen);

	}

	@Override
	public void onUpdateProgress(TaskBean updateProgress) {
		HomeAdapter.MyViewHolder myViewHolder = mholderMap.get(updateProgress);
		if (myViewHolder != null) {
			myViewHolder.item_pb.setProgress(updateProgress.progress);
			mHomeAdapter.notifyItemChanged(myViewHolder.item_pb.getId());
		}
	}

	class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.MyViewHolder> {

		@Override
		public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			MyViewHolder holder = new MyViewHolder(LayoutInflater.from(
					ServerActivity.this).inflate(R.layout.item_list, parent,
					false));
			return holder;
		}

		@Override
		public void onBindViewHolder(MyViewHolder holder, int position) {
			// 设置数据
			TaskBean taskBean = mDataList.get(position);
			mHolderList.add(holder);
			mholderMap.put(taskBean, holder);
			holder.item_tv_file_name.setText(taskBean.fileName);
			holder.item_tv_ip.setText(taskBean.ip);
			holder.item_tv_wifi_mac.setText(taskBean.wifiMac);
			int size = taskBean.fileLen / 1024;
			if (size == 0)
				size += 1;
			holder.item_tv_file_len.setText(size + "kb");
			holder.item_pb.setMax(taskBean.fileLen);
		}

		@Override
		public int getItemCount() {
			if (mDataList != null) {
				return mDataList.size();
			}
			return 0;
		}

		class MyViewHolder extends RecyclerView.ViewHolder {

			public TextView item_tv_ip;
			public TextView item_tv_wifi_mac;
			public TextView item_tv_file_name;
			public ProgressBar item_pb;
			public TextView item_tv_file_len;
			public MyViewHolder(View rootView) {
				super(rootView);
				this.item_tv_ip = (TextView) rootView
						.findViewById(R.id.item_tv_ip);
				this.item_tv_wifi_mac = (TextView) rootView
						.findViewById(R.id.item_tv_wifi_mac);
				this.item_tv_file_name = (TextView) rootView
						.findViewById(R.id.item_tv_file_name);
				this.item_tv_file_len = (TextView) rootView
						.findViewById(R.id.item_tv_file_len);
				this.item_pb = (ProgressBar) rootView
						.findViewById(R.id.item_pb);
			}
		}

	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		SocketManager.getInstance().closeServer();
	}

	private void initView() {
		et_client_msg = (EditText) findViewById(R.id.et_client_msg);
		btn_clear = (Button) findViewById(R.id.btn_clear);
		btn_clear.setOnClickListener(this);
		btn_restart_server = (Button) findViewById(R.id.btn_restart_server);
		btn_restart_server.setOnClickListener(this);
		btn_send = (Button) findViewById(R.id.btn_send);
		btn_send.setOnClickListener(this);
		et_error_msg = (EditText) findViewById(R.id.et_error_msg);
		et_error_msg.setOnClickListener(this);
		btn_send_broadcast = (Button) findViewById(R.id.btn_send_broadcast);
		btn_send_broadcast.setOnClickListener(this);
		list_view = (ListView) findViewById(R.id.list_view);

		// mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		btn_clear_dir_broadcast = (Button) findViewById(R.id.btn_clear_dir_broadcast);
		btn_clear_dir_broadcast.setOnClickListener(this);
	}

	private ArrayList<View> viewList = new ArrayList<>();
	public BaseAdapter mBaseAdapter = new BaseAdapter() {

		class ViewHolder {
			public View rootView;
			public TextView item_tv_ip;
			public TextView item_tv_wifi_mac;
			public TextView item_tv_file_name;
			public TextView item_tv_file_len;
			public ProgressBar item_pb;

			public ViewHolder(View rootView) {
				this.rootView = rootView;
				this.item_tv_ip = (TextView) rootView
						.findViewById(R.id.item_tv_ip);
				this.item_tv_wifi_mac = (TextView) rootView
						.findViewById(R.id.item_tv_wifi_mac);
				this.item_tv_file_name = (TextView) rootView
						.findViewById(R.id.item_tv_file_name);
				this.item_tv_file_len = (TextView) rootView
						.findViewById(R.id.item_tv_file_len);
				this.item_pb = (ProgressBar) rootView
						.findViewById(R.id.item_pb);
			}

		}

		@Override
		public int getCount() {
			if (mDataList != null) {
				return mDataList.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return mDataList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = View.inflate(ServerActivity.this,
						R.layout.item_list, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// 设置数据
			TaskBean taskBean = mDataList.get(position);
			convertView.setTag(taskBean.hashCode(), position);
			viewList.add(convertView);
			holder.item_tv_file_name.setText(taskBean.fileName);
			holder.item_tv_ip.setText(taskBean.ip);
			holder.item_tv_wifi_mac.setText(taskBean.wifiMac);
			int size = taskBean.fileLen / 1024;
			if (size == 0)
				size += 1;
			holder.item_tv_file_len.setText(size + "kb");
			holder.item_pb.setMax(taskBean.fileLen);
			return convertView;
		}
	};
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_clear :
				et_error_msg.setText("");

				break;
			case R.id.btn_restart_server :

				SocketManager.getInstance().startServer(mPaths, this);
				break;
			case R.id.btn_send :
				Intent i = new Intent(ServerActivity.this,
						FilePickerActivity.class);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
				i.putExtra(FilePickerActivity.EXTRA_MODE,
						FilePickerActivity.MODE_FILE);
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment
						.getExternalStorageDirectory().getPath());

				startActivityForResult(i, FILE_CODE);
				break;
			case R.id.btn_send_broadcast :
				if (mPaths==null|| mPaths.size() == 0) {
					Toast.makeText(ServerActivity.this, "未选择发送文件路径",
							Toast.LENGTH_SHORT).show();
					return;
				}
				// String sendMessage = WifiUtils.getInstance()
				// .getParsedIp()
				// + "=="
				// + file.getName()
				// + "==" + file.length();
				BroadcastBean broadcastBean = new BroadcastBean();
				List<BroadcastBean.FilesBean> fileList = new ArrayList<>();

				for (int j = 0; j < mPaths.size(); j++) {
					File file = new File(mPaths.get(j));
					BroadcastBean.FilesBean filesBean = new BroadcastBean.FilesBean(
							file.getName(), file.length());
					fileList.add(filesBean);
				}
				broadcastBean.files = fileList;
				broadcastBean.hostIp = WifiUtils.getInstance().getParsedIp();
				broadcastBean.type = BroadcastBean.TYPE_FILE;
				String sendMessage = SocketManager.getInstance().parseJson(
						broadcastBean);
				byte[] sendMSG = sendMessage.getBytes();
				sendBroadcast(sendMSG);

				break;
			case R.id.btn_clear_dir_broadcast :
				// String msg = "clear";
				BroadcastBean clearBean = new BroadcastBean();
				clearBean.type = BroadcastBean.TYPE_CLEAR;
				String json = SocketManager.getInstance().parseJson(clearBean);
				sendBroadcast(json.getBytes());
				break;
		}
	}

	private void sendBroadcast(final byte[] sendMSG) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		//
		// }
		//
		// }).start();
		new MulticastThread(sendMSG).start();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
			if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE,
					true)) {
				// For JellyBean and above
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					ClipData clip = data.getClipData();
					mPaths = new ArrayList<>();
					if (clip != null) {
						for (int i = 0; i < clip.getItemCount(); i++) {
							Uri uri = clip.getItemAt(i).getUri();
							mPaths.add(uri.getPath());
						}
							SocketManager.getInstance().startServer(mPaths,
									this);

					}
				}
				// else {
				// final ArrayList<String> paths = data
				// .getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);
				// final ArrayList<String> fileNames = new ArrayList<>();
				// if (paths != null) {
				// for (String path : paths) {
				// Uri uri = Uri.parse(path);
				// paths.add(uri.getPath());
				// fileNames.add(uri.getLastPathSegment());
				//
				// }
				//
				// }
				// }

			}
		}
	}

	@Override
	public void onProgress(TaskBean taskBean, int progress) {
		// DebugLogger.d(taskBean.ip + ": " + progress);

		updateProgress(taskBean, progress);
		// for (int i = 0; i < mHolderList.size(); i++) {
		// HomeAdapter.MyViewHolder myViewHolder = mHolderList.get(i);
		//
		// if
		// (myViewHolder.item_tv_wifi_mac.getText().toString().equals(taskBean.wifiMac)){
		// myViewHolder.item_pb.setProgress(progress);
		// mHomeAdapter.notifyItemChanged(myViewHolder.item_pb.getId());
		// break;
		//
		// }
		// }

	}

	private void updateProgress(TaskBean taskBean, int progress) {
		View view = null;
		for (int i = 0; i < viewList.size(); i++) {
			if (viewList.get(i).getTag(taskBean.hashCode()) != null) {
				view = viewList.get(i);
				break;
			}
		}
		if (view != null) {
			ServerAdapter.ViewHolder viewHolder = (ServerAdapter.ViewHolder) view
					.getTag();
			viewHolder.item_pb.setProgress(progress);
		}
	}
}
