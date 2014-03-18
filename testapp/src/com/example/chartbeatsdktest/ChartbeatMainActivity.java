package com.example.chartbeatsdktest;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import com.chartbeat.androidsdk.Tracker;

public class ChartbeatMainActivity extends Activity {
	private static final String TAG = "ChartbeatMainActivity";
	private static final String VIEW_ID = "A VIEW ID";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chartbeat_main);
		Tracker.startTrackerWithAccountId("ACCOUNT_ID", this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chartbeat_main, menu);
		return true;
	}
	
	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		Tracker.userInteracted();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Tracker.trackView(VIEW_ID, null);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Tracker.userLeftView(VIEW_ID);
	}
}
