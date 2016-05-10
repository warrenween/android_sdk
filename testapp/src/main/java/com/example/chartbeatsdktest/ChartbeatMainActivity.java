package com.example.chartbeatsdktest;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;

import com.chartbeat.androidsdk.Tracker;

/**
 * This is a sample Application for demonstrating and testing
 * the Chartbeat SDK. This minimal implementation
 * shows how to track views, user interaction, and a user entering/leaving the app.
 * 
 * 
 * @author bjorn
 *
 */
public class ChartbeatMainActivity extends Activity {
	private static final String TAG = "ChartbeatMainActivity";
	private static final String VIEW_ID = "A_VIEW_ID";
	private static final String VIEW_TITLE = "Test View";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chartbeat_main);
		Tracker.DEBUG_MODE = true;
		final MainScrollView msv = (MainScrollView) getWindow().getDecorView().findViewById(R.id.scrollView1);
		msv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                System.out.println("------- " + this);
                Tracker.trackView(ChartbeatMainActivity.this, VIEW_ID, VIEW_TITLE,
                        msv.getScrollPosition(),
                        msv.getContentHeight(),
                        msv.getViewHeight(),
                        msv.getWidth());
            }
		        });
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
		MainScrollView msv = (MainScrollView) getWindow().getDecorView().findViewById(R.id.scrollView1);
		
		if( msv.getContentHeight() != 0 ) {
			Tracker.trackView(ChartbeatMainActivity.this, VIEW_ID, VIEW_TITLE,
					msv.getScrollPosition(),
					msv.getContentHeight(),
					msv.getViewHeight(),
					msv.getWidth() );
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Tracker.userLeftView(VIEW_ID);
	}
	
	public void simulateTyping(View view) {
		Log.d(TAG, "Typing");
		Tracker.userTyped();
	}
	
	public void switchViews(View view) {
		Log.d(TAG, "Switching Views");
		Intent intent = new Intent(this,AltActivity.class);
		startActivity(intent);
	}
}
