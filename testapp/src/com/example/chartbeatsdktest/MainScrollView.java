package com.example.chartbeatsdktest;

import com.chartbeat.androidsdk.Tracker;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

public final class MainScrollView extends ScrollView {
	String TAG = "Chartbeat scrollbar";

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
		Tracker.setPosition( t, (this.getChildAt(0).getHeight()-this.getHeight()), -1, getWidth() );
	}
}
