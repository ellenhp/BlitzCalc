package com.blitzcalc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
	}
	
	public void onClickViewLicense(View v) {
		Intent intent = new Intent(this, ApacheActivity.class);
		startActivity(intent);
	}
}
