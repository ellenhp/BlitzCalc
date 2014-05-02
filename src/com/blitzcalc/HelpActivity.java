package com.blitzcalc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
	}
	
	public void onClickAbout(View v) {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}
}
