package com.blitzcalc;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class GameOverFragment extends Fragment {

	View v = null;
	int goal;
	double score = 0;
	double recordTime = 0;
	boolean isChallenge = false;
	GameOverListener listener;

	private static final String format = "0.00";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.fragment_game_over, container, false);

		if (listener == null && getActivity() instanceof GameOverListener) {
			listener = (GameOverListener) getActivity();
		}

		setText();

		v.findViewById(R.id.buttonBackToMainMenu).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		v.findViewById(R.id.buttonBackToNewGame).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onNewGame();
				}
			}
		});
		v.findViewById(R.id.buttonGameOverLeaderboards).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onDisplayScoreboards(goal);
				}
			}
		});

		ConnectivityManager connec = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean online=(connec != null && (connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED));

		if (online && PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("sign_in", true)) {
			if (!isChallenge) {
				v.findViewById(R.id.buttonGameOverLeaderboards).setVisibility(View.INVISIBLE);
			}
		}
		else {
			v.findViewById(R.id.buttonGameOverLeaderboards).setVisibility(View.INVISIBLE);
		}
		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		v = null;
	}

	public void setText() {
		if (v != null) {
			String scoreText = new DecimalFormat(format).format(score) + "s";
			String recordText;
			String text = getString(R.string.score_base_string) + scoreText;
			if (isChallenge) {
				if (recordTime <= 0) {
					recordText = getString(R.string.not_available);
				} else {
					recordText = new DecimalFormat(format).format(recordTime) + "s";
				}
				text = text + "\n" + getString(R.string.current_global_record) + recordText;
			}
			((TextView) v.findViewById(R.id.textViewDisplayScore)).setText(text);
		}
	}

	public void setScore(int goal, double time, double recordTime) {
		this.score = time;
		this.goal = goal;
		this.recordTime = recordTime;
		setText();
	}

	public void setDisplayLeaderboardButton(boolean display) {
		isChallenge = display;
		if (v != null) {
			if (display) {
				v.findViewById(R.id.buttonGameOverLeaderboards).setVisibility(View.VISIBLE);

			} else {
				v.findViewById(R.id.buttonGameOverLeaderboards).setVisibility(View.INVISIBLE);
			}
		}
	}

	public void setListener(GameOverListener l) {
		listener = l;
	}
}
