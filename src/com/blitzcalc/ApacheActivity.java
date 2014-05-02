package com.blitzcalc;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ApacheActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_apache);
		
		WebView wv = (WebView) findViewById(R.id.webViewApache);
		wv.loadUrl("file:///android_asset/apache_2.0.html");
	}
}
