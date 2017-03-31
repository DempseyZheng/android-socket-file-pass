package com.accvmedia.mysocket;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.accvmedia.mysocket.jobqueue.SocketJobManager;
import com.accvmedia.mysocket.socket.SocketManager;
import com.accvmedia.mysocket.util.WifiUtils;

public class MainActivity extends AppCompatActivity
		implements
			View.OnClickListener {

	private Button btn_server;
	private Button btn_client;
	private TextView tv_ip;
	private TextView tv_version;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		WifiUtils.getInstance().init(this);
SocketJobManager.getInstance().init(this);

		getSupportActionBar().setTitle(
				"\t\t" +"路由器mac地址:"+WifiUtils.getInstance().getBSSID());
		initView();
		SocketManager.getInstance().init();
		tv_version.setText("版本号: "+BuildConfig.VERSION_NAME);



	}

	private void initView() {
		btn_server = (Button) findViewById(R.id.btn_server);
		btn_client = (Button) findViewById(R.id.btn_client);

		btn_server.setOnClickListener(this);
		btn_client.setOnClickListener(this);
		tv_ip = (TextView) findViewById(R.id.tv_ip);
		tv_ip.setOnClickListener(this);
		tv_ip.setText(WifiUtils.getInstance().getParsedIp());
		tv_version = (TextView) findViewById(R.id.tv_version);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_server :
				startActivity(new Intent(MainActivity.this,
						ServerActivity.class));
				break;
			case R.id.btn_client :
				startActivity(new Intent(MainActivity.this,
						ClientActivity.class));

				break;
		}
    }
}
