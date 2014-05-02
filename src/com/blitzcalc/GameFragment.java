package com.blitzcalc;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class GameFragment extends Fragment implements OnCancelListener, KeypadListener {

	private long goal;

	private Keypad keypad;
	private TextView timeOutputView;

	private Timer timer;

	private boolean isGameStarted = false, isGameEnded = false;
	private long timeStart;

	private Dialog dialog;
	private TextView dialogTextView;
	private View v;

	private GameListener listener = null;

	private boolean isChallenge;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.fragment_game, container, false);

		if (isChallenge) {
			v.findViewById(R.id.textViewTime).setVisibility(View.VISIBLE);
		} else {
			v.findViewById(R.id.textViewTime).setVisibility(View.INVISIBLE);
		}

		timeOutputView = (TextView) v.findViewById(R.id.textViewTime);

		if (savedInstanceState != null) {
			goal = savedInstanceState.getLong("goal");
			timeStart = (Long) savedInstanceState.getLong("gameStart");
			isGameStarted = savedInstanceState.getBoolean("started");
			isGameEnded = savedInstanceState.getBoolean("ended");
			((TextView) v.findViewById(R.id.textViewGoal)).setText(goal + getString(R.string.goal_base_text));

			startTimer();
		} else {
			keypad = new Keypad();
			keypad.setKeypadListener(this);
			getFragmentManager().beginTransaction().add(R.id.keypad_fragment_container, keypad).commit();

			startGame();
		}

		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putLong("goal", goal);
		outState.putLong("gameStart", timeStart);
		outState.putBoolean("started", isGameStarted);
		outState.putBoolean("ended", isGameEnded);
	}

	@Override
	public void onResume() {
		super.onResume();
		startTimer();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopTimer();
	}

	@Override
	public void onDestroy() {
		v = null;
		super.onDestroy();
	}

	private void stopTimer() {
		timer.cancel();
		timer = null;
	}

	private void startTimer() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (getActivity() == null) {
					return;
				}
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateTime();
					}
				});
			}
		}, 100, 100);
	}

	protected void startGame() {
		isGameStarted = isGameEnded = false;
		((TextView) v.findViewById(R.id.textViewGoal)).setText(goal + getString(R.string.goal_base_text));
		keypad.clearAll();

		timeStart = System.nanoTime();

		if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_ready", true)) {
			dialogTextView = new TextView(getActivity());
			dialogTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.button_text_size));
			dialogTextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
			dialogTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			dialogTextView.setEllipsize(null);
			dialogTextView.setMaxLines(10);
			dialogTextView.setHorizontallyScrolling(false);

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setView(dialogTextView);
			dialog = b.create();

			dialog.setCanceledOnTouchOutside(true);
			dialog.setOnCancelListener(this);

			dialog.getWindow().setWindowAnimations(R.style.DialogNoAnimation);
			dialog.show();

			isGameEnded = isGameStarted = false;
		} else {
			isGameEnded = false;
			isGameStarted = true;
		}
		startTimer();
		updateTime();
	}

	@Override
	public void onNewResult(long l) {
		if (l == goal) {
			isGameEnded = true;
			onWin((System.nanoTime() - timeStart) / 1000000000.0);
		}
	}

	protected void onWin(double time) {
		listener.onWin((int) goal, time);
	}

	private void updateTime() {
		if (v == null) {
			return;
		}
		try {
			if (!isGameEnded && isGameStarted) {
				double elapsed = (System.nanoTime() - timeStart) / 1000000000.0;
				String timeText = new DecimalFormat("0.0").format(elapsed);
				timeOutputView.setText(timeText);
			} else if (!isGameEnded && !isGameStarted) {
				double countDownFrom = 3;
				double elapsed = (System.nanoTime() - timeStart) / 1000000000.0;
				timeOutputView.setText("");
				if (elapsed > countDownFrom) {
					isGameStarted = true;
					if (dialog != null && dialog.isShowing()) {
						dialog.cancel();
					}
					timeStart = System.nanoTime();
				} else {
					dialogTextView.setText(getString(R.string.goal_prompt_text) + goal + "\n" + getString(R.string.ready_message) + (int) (countDownFrom - elapsed + 1));
				}
			}
		} catch (Exception ex) {
			Log.e("game", "exception updating time");
		}
	}

	@Override
	public void onCancel(DialogInterface dialogInterface) {
		if (!isGameStarted && listener != null) {
			isGameStarted = true;
			dialog.cancel();
			timeStart = System.nanoTime();
		}
	}

	protected int getGoal() {
		return (int) goal;
	}

	public void setGoal(int goal) {
		this.goal = goal;
		if (v != null) {
			startGame();
		}
	}

	public void setIsChallenge(boolean isChallenge) {
		this.isChallenge = isChallenge;
		if (v != null) {
			if (isChallenge) {
				v.findViewById(R.id.textViewTime).setVisibility(View.VISIBLE);
			} else {
				v.findViewById(R.id.textViewTime).setVisibility(View.INVISIBLE);
			}
		}
	}

	public void setListener(GameListener l) {
		listener = l;
	}
}
