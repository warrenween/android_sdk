package com.example.chartbeatsdktest;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

import com.chartbeat.androidsdk.Tracker;

public final class MainScrollView extends ScrollView {
	private static final String TAG = "Chartbeat scrollbar";
	private int scrollPosition = 0;

	public MainScrollView(Context context) {
		super(context);
	}

	public MainScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MainScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onScrollChanged(int l, int t, int oldl, int oldt) {
		Log.d(TAG,"Scrolled: " + l + " : " + t );
		Log.d(TAG,"M: " + (this.getChildAt(0).getHeight()-this.getHeight()) );
		Tracker.setPosition( t,
				getContentHeight(),
				getViewHeight(),
				getWidth() );
		scrollPosition = t;
	}
	
	public int getScrollPosition() {
		return scrollPosition;
	}
	
	public int getContentHeight() {
		return this.getHeight();
	}
	
	public int getViewHeight() {
		return this.getChildAt(0).getHeight();
	}
}
