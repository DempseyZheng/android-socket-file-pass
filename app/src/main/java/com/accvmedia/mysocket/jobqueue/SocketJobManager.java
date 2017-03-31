package com.accvmedia.mysocket.jobqueue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.accvmedia.mysocket.util.DebugLogger;
import com.birbit.android.jobqueue.AsyncAddCallback;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.callback.JobManagerCallback;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;

import java.util.Set;

/**
 * Created by dempseyZheng on 2017/3/31
 */
public class SocketJobManager implements JobManagerCallback {
	private static SocketJobManager mSocketJobManager = null;
	private JobManager mJobManager;
	static {
		mSocketJobManager = new SocketJobManager();
	}

	public static SocketJobManager getInstance() {
		return mSocketJobManager;
	}

	public void init(Context context) {
		Configuration.Builder builder = new Configuration.Builder(context)
				.customLogger(new CustomLogger() {
					private static final String TAG = "JOBS";
					@Override
					public boolean isDebugEnabled() {
						return DebugLogger.isDebuggable();
					}

					@Override
					public void d(String text, Object... args) {
						DebugLogger.d(String.format(text, args));
					}

					@Override
					public void e(Throwable t, String text, Object... args) {
						DebugLogger.e(String.format(text, args), t);
					}

					@Override
					public void e(String text, Object... args) {
						DebugLogger.e(String.format(text, args));
					}

					@Override
					public void v(String text, Object... args) {
						DebugLogger.v(String.format(text, args));
					}
				}).minConsumerCount(1)// always keep at least one consumer alive
				.maxConsumerCount(1)// up to 3 consumers at a time
				.loadFactor(1)// 3 jobs per consumer
				.consumerKeepAlive(120);// wait 2 minute

		mJobManager = new JobManager(builder.build());
		mJobManager.addCallback(this);
	}

	@Override
	public void onJobAdded(@NonNull Job job) {
		Set<String> tags = job.getTags();
		for (String tag : tags) {
			Log.e("Dempsey", "onJobAdded: " + tag);
		}
	}

	@Override
	public void onJobRun(@NonNull Job job, int resultCode) {

	}

	@Override
	public void onJobCancelled(@NonNull Job job, boolean byCancelRequest,
			@Nullable Throwable throwable) {

	}

	@Override
	public void onDone(@NonNull Job job) {
		Set<String> tags = job.getTags();
		for (String tag : tags) {
			Log.e("Dempsey", "onDone: " + tag);
		}
	}

	@Override
	public void onAfterJobRun(@NonNull Job job, int resultCode) {

	}

	public void addJobInBackground(Job job, final AsyncAddCallback callback) {
		if (mJobManager != null) {
			mJobManager.addJobInBackground(job, callback);
		}
	}

	public void addJobInBackground(Job job) {
		if (mJobManager != null) {
			mJobManager.addJobInBackground(job);
		}
	}
}
