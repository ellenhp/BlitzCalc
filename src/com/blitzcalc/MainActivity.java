package com.blitzcalc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.example.games.basegameutils.GameHelper;
import com.google.example.games.basegameutils.GameHelper.GameHelperListener;

public class MainActivity extends FragmentActivity implements GameHelperListener {

	GameHelper helper;
	boolean updating = false;
	boolean signingIn = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	private void setupHelper() {
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result == ConnectionResult.SUCCESS && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true)) {
			ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (!signingIn && connec != null && (connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)) {
				helper = new GameHelper(this, GameHelper.CLIENT_GAMES);
				helper.setMaxAutoSignInAttempts(0);
				helper.setup(this);
				helper.beginUserInitiatedSignIn();
				signingIn = true;
			}
		} else {
			if (!updating) {
				updating = true;
				GooglePlayServicesUtil.showErrorDialogFragment(result, this, 101, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("play_services_opt_out", true).putBoolean("sign_in", false).apply();
					}
				});
			} else {
				PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("play_services_opt_out", true).putBoolean("sign_in", false).apply();
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (helper != null) {
			helper.onStart(this);
		}
	}

	@Override
	protected void onResume() {
		boolean optIn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("play_services_opt_out", false)
				&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true);
		if (helper == null && (optIn || !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("play_services_opt_out", false))) {
			setupHelper();
		} else if (helper != null && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true)) {
			helper.signOut();
			helper = null;
		}
		updating = false;
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (helper != null) {
			helper.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (helper != null) {
			helper.onStop();
		}
	}

	public void onClickChallenge(View v) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra("challenge", true);
		startActivity(intent);
	}

	public void onClickPractice(View v) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra("challenge", false);
		startActivity(intent);
	}

	public void onClickSettings(View v) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void onClickHelp(View v) {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
	}

	@Override
	public void onSignInFailed() {
		Toast.makeText(this, R.string.play_services_error, Toast.LENGTH_SHORT).show();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("sign_in", false).apply();
		helper = null;
		Log.d("scores", "sign in from main menu failed");
		signingIn = false;
	}

	@Override
	public void onSignInSucceeded() {
		signingIn = false;
	}
}
