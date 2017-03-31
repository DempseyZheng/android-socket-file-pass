package com.accvmedia.mysocket.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by Administrator on 2016/11/24 0024.
 */

public class WifiChangedReceiver extends BroadcastReceiver {
	private static final boolean DEBUG = true;

	@Override
	public void onReceive(Context context, Intent intent) {

		if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			Parcelable parcelableExtra = intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (null != parcelableExtra) {
				NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
				NetworkInfo.State state = networkInfo.getState();
				switch (state) {
					case CONNECTED :
//						SocketManager.getInstance().newClient(Constant.HOST_IP,
//								Constant.PORT);
						break;

					case DISCONNECTED :

						break;
				}
			}
		}

	}
}
