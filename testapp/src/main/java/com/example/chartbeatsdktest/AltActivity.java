package com.example.chartbeatsdktest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import com.chartbeat.androidsdk.Tracker;

import java.util.HashSet;

public class AltActivity extends Activity {
	private static final String VIEW_ID = "ANOTHER_VIEW_ID";
	private static final String VIEW_TITLE = "Different Test View";
	private static final String TAG = "Chartbeat Alt Activity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alt);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.alt, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// ------------- Respond to user input

	public void onSectionClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		if (checked) {
			switch (view.getId()) {
			case R.id.sectiona:
				Log.d(TAG, "Section A");
				Tracker.setSections( "Section A" );
				break;
			case R.id.sectionb:
				Log.d(TAG, "Section B");
				Tracker.setSections( "Section B" );
				break;
			}
		}
	}

	public void onAuthorClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		if (checked) {
			switch (view.getId()) {
			case R.id.author1:
				Log.d(TAG, "Author 1");
				Tracker.setAuthors( "Author A" );
				break;
			case R.id.author2:
				Log.d(TAG, "Author 2");
				HashSet<String> authors = new HashSet<String>();
				authors.add("Author B");
				authors.add("Author C");
				Tracker.setAuthors( authors );
				break;
			}
		}
	}

	public void onZoneClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		if (checked) {
			switch (view.getId()) {
			case R.id.zonea:
				Log.d(TAG, "Zone A");
				Tracker.setZones( "Zone A" );
				break;
			case R.id.zoneb:
				Log.d(TAG, "Zone B");
				Tracker.setZones( "Zone B" );
				break;
			}
		}
	}

	// ------------- Chartbeat tracker implementation

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		Tracker.userInteracted();
	}

	@Override
	public void onResume() {
		super.onResume();
		Tracker.trackView(this, VIEW_ID, VIEW_TITLE);
		//simulate view loading a half second later:
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Tracker.setViewLoadTime(.5f);
			}
		}, 500 );
	}

	@Override
	public void onPause() {
		super.onPause();
		Tracker.userLeftView(VIEW_ID);
	}

}
