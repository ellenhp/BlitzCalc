package com.blitzcalc;

import java.util.ArrayList;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.games.achievement.Achievements.UpdateAchievementResult;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadScoresResult;
import com.google.example.games.basegameutils.GameHelper;
import com.google.example.games.basegameutils.GameHelper.GameHelperListener;

public class GameActivity extends FragmentActivity implements GameListener, GameOverListener, GameHelperListener {

	private boolean isChallenge = false;

	GameFragment gameFrag = new GameFragment();
	GameOverFragment gameOverFrag = new GameOverFragment();

	GameHelper helper;
	ArrayList<Runnable> onSignIn = new ArrayList<Runnable>();
	Random rand;

	private double recordTime = 0;

	private int goal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result == ConnectionResult.SUCCESS && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true)) {
			ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connec != null && (connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)) {
				helper = new GameHelper(this, GameHelper.CLIENT_GAMES);
				helper.setMaxAutoSignInAttempts(0);
				helper.setup(this);
			}
		}

		if (getIntent().getExtras() != null) {
			isChallenge = getIntent().getExtras().getBoolean("challenge");
		}

		rand = new Random();

		// FRAGMENTS

		if (savedInstanceState == null) {
			gameFrag = new GameFragment();
			gameFrag.setListener(this);
			setModeGame();

			gameOverFrag = new GameOverFragment();
			gameOverFrag.setListener(this);
		}

	}

	public void refreshNumber() {
		if (!isChallenge) {
			int maxDigits = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("digits", "3")) - 1;
			goal = (int) (rand.nextDouble() * 0.9 * Math.pow(10, maxDigits) + Math.pow(10, maxDigits - 1));
		} else {
			goal = LeaderboardManager.getInstance(this).getRandomGoalNumber(PreferenceManager.getDefaultSharedPreferences(this).getInt("rounds_finished", 0));
		}
		gameFrag.setGoal(goal);
		recordTime = 0;

		Runnable r = new Runnable() {
			@Override
			public void run() {
				String id = LeaderboardManager.getInstance(GameActivity.this).getIdForGoal(goal);
				PendingResult<LoadScoresResult> result = Games.Leaderboards.loadTopScores(helper.getApiClient(), id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC, 1);
				result.setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {
					@Override
					public void onResult(LoadScoresResult arg0) {
						if (arg0.getScores().getCount() > 0) {
							recordTime = arg0.getScores().get(0).getRawScore() / 1000.0;
						} else {
							recordTime = -10;
						}
					}
				});
			}
		};

		if (isChallenge && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true)) {
			if (helper != null && helper.isSignedIn()) {
				r.run();
			} else if (helper != null) {
				onSignIn.add(r);
			}
		}
		gameFrag.setIsChallenge(isChallenge);
	}

	public void onClickRefreshGoal(View v) {
		refreshNumber();
	}

	public void setModeGame() {
		refreshNumber();
		getSupportFragmentManager().beginTransaction().replace(R.id.game_fragment_container, gameFrag).commit();
	}

	public void setModeGameOver() {
		getSupportFragmentManager().beginTransaction().replace(R.id.game_fragment_container, gameOverFrag).commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_screen_on", false)) {
			getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onStart() {
		if (helper != null) {
			helper.onStart(this);
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		if (helper != null) {
			helper.onStop();
		}
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onWin(int goal, double time) {
		if (isChallenge && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("play_services_opt_out", false)) {
			ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			boolean online = false;
			if (connec != null && (connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)) {
				online = true;
			}
			boolean allowedToSubmit = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("submit_scores", false)
					&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true);
			if (online && allowedToSubmit && helper != null && helper.isSignedIn()) {
				Log.d("scores", "submitting score");
				doScoreSubmission(goal, time);
			} else if (online && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("submit_scores_was_picked", false)) {
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle(R.string.submit_scores_title);
				b.setPositiveButton(android.R.string.yes, new SubmitScoreClickListener(goal, time));
				b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						PreferenceManager.getDefaultSharedPreferences(GameActivity.this).edit().putBoolean("submit_scores", false).apply();
						PreferenceManager.getDefaultSharedPreferences(GameActivity.this).edit().putBoolean("submit_scores_was_picked", true).apply();
					}
				});
				b.create().show();
			}
		}

		doAchievements(time, recordTime);
		gameOverFrag.setScore(goal, time, recordTime);
		gameOverFrag.setDisplayLeaderboardButton(isChallenge);
		setModeGameOver();
		Log.d("game", "switching to game over fragment");
	}

	@Override
	public void onNewGame() {
		setModeGame();
		Log.d("game", "switching to game fragment");
	}

	@Override
	public void onDisplayScoreboards(int goal) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true) && helper != null && helper.isSignedIn()) {
			Log.d("scores", "displaying scoreboards");
			startActivityForResult(Games.Leaderboards.getLeaderboardIntent(helper.getApiClient(), LeaderboardManager.getInstance(this).getIdForGoal(goal)), 0);
		} else {
			Log.d("scores", "not signed in, not displaying scoreboards");
		}
	}

	@Override
	public void onRequestSubmitScore(int goal, double time) {
		Log.d("scores", "requested submission of first time score");
		if (helper != null && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sign_in", true)) {
			if (!helper.isSignedIn()) {
				onSignIn.add(new SubmitScoreRunnable(goal, time));
				Log.d("scores", "signing in to submit a score");
				helper.beginUserInitiatedSignIn();
			} else {
				doScoreSubmission(goal, time);
				Log.d("scores", "submitted score");
			}
		}
	}

	@Override
	public void onSignInFailed() {
		Log.d("scores", "sign-in failed");
		onSignIn.clear();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("sign_in", false).apply();
		helper = null;
	}

	@Override
	public void onSignInSucceeded() {
		Log.d("scores", "sign-in succeeded");
		for (Runnable r : onSignIn) {
			runOnUiThread(r);
		}
		onSignIn.clear();
	}

	protected void doAchievements(double time, double recordTime) {
		if (helper == null || !helper.isSignedIn()) {
			return;
		}
		ResultCallback<Achievements.UpdateAchievementResult> callback = new ResultCallback<Achievements.UpdateAchievementResult>() {
			@Override
			public void onResult(UpdateAchievementResult arg0) {
				Log.d("scores", arg0.getAchievementId() + " " + arg0.getStatus().toString());
			}
		};

		boolean offline = (recordTime == 0);
		if (!offline) {
			if (time < recordTime || recordTime < 0) {
				Games.Achievements.unlockImmediate(helper.getApiClient(), getString(R.string.achievement_be_the_best)).setResultCallback(callback);
			}
		}
		int roundNumber = PreferenceManager.getDefaultSharedPreferences(this).getInt("rounds_finished", 0);
		roundNumber++;
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("rounds_finished", roundNumber).apply();
		if (roundNumber >= 1) {
			Games.Achievements.unlockImmediate(helper.getApiClient(), getString(R.string.achievement_first_round)).setResultCallback(callback);
		}
		if (roundNumber >= 10) {
			Games.Achievements.unlockImmediate(helper.getApiClient(), getString(R.string.achievement_go_10_rounds)).setResultCallback(callback);
		}
		if (roundNumber >= 25) {
			Games.Achievements.unlockImmediate(helper.getApiClient(), getString(R.string.achievement_go_25_rounds)).setResultCallback(callback);
		}
	}

	protected void doScoreSubmission(int goal, double score) {
		Games.Achievements.unlock(helper.getApiClient(), getString(R.string.achievement_on_the_scoreboard));
		Games.Leaderboards.submitScore(helper.getApiClient(), LeaderboardManager.getInstance(this).getIdForGoal(goal), (int) (score * 1000));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (helper != null) {
			helper.onActivityResult(requestCode, resultCode, data);
		}
	}

	private class SubmitScoreRunnable implements Runnable {
		private int goalToSubmitFor;
		private double scoreToSubmit;

		public SubmitScoreRunnable(int goal, double time) {
			goalToSubmitFor = goal;
			scoreToSubmit = time;
		}

		@Override
		public void run() {
			doScoreSubmission(goalToSubmitFor, scoreToSubmit);
			Log.d("scores", "submitted first-time score");
		}
	}

	private class SubmitScoreClickListener implements DialogInterface.OnClickListener {
		private int goal;
		private double time;

		public SubmitScoreClickListener(int goal, double time) {
			this.goal = goal;
			this.time = time;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Log.d("scores", "yes to submit scores");
			PreferenceManager.getDefaultSharedPreferences(GameActivity.this).edit().putBoolean("sign_in", true).apply();
			PreferenceManager.getDefaultSharedPreferences(GameActivity.this).edit().putBoolean("submit_scores", true).apply();
			PreferenceManager.getDefaultSharedPreferences(GameActivity.this).edit().putBoolean("submit_scores_was_picked", true).apply();
			onSignIn.add(new SubmitScoreRunnable(goal, time));
			if (helper == null) {
				helper = new GameHelper(GameActivity.this, GameHelper.CLIENT_GAMES);
				helper.setMaxAutoSignInAttempts(0);
				helper.setup(GameActivity.this);
			}
			helper.beginUserInitiatedSignIn();
			Log.d("scores", "began sign-in");
		}
	}
}
