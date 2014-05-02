package com.blitzcalc;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

public class LeaderboardManager {
	private static LeaderboardManager instance;

	private SparseArray<String> leaderboardIDs = new SparseArray<String>();
	private List<Integer> keys = new ArrayList<Integer>();

	private Random rand;

	private Queue<Integer> lastKeys = new ArrayDeque<Integer>();

	private LeaderboardManager(Context cx) {
		rand = new Random();
		try {
			InputStream stream = cx.getResources().openRawResource(R.raw.scoreboard_ids);
			byte[] b = new byte[stream.available()];
			stream.read(b);
			int key = 0;
			for (String line : new String(b).split("\n")) {
				String tokens[] = line.split(" ");
				if (tokens.length != 2) {
					Log.e("scores", "improper scoreboard id list");
					break;
				}
				key = Integer.parseInt(tokens[0]);
				leaderboardIDs.put(key, tokens[1]);
				keys.add(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * String tokens[] = str.split(" "); if (tokens.length!=2) { Log.e("scores",
	 * "improper scoreboard id list"); break; } key=Integer.parseInt(tokens[0]);
	 * leaderboardIDs.put(key, tokens[1]); keys.add(key); str=sc.nextLine();
	 */

	public int getRandomGoalNumber(int roundNumber) {
		int key;
		boolean babyMode = roundNumber <= 2; // first 3 rounds are in baby mode
		do {
			key = keys.get(rand.nextInt(keys.size()));
		} while (lastKeys.contains(key) || (key > 100 && babyMode));
		if (lastKeys.size() > 10) {
			lastKeys.poll();
		}
		lastKeys.add(key);
		return key;
	}

	public String getIdForGoal(int key) {
		return leaderboardIDs.get(key);
	}

	public static LeaderboardManager getInstance(Context cx) {
		if (instance == null) {
			instance = new LeaderboardManager(cx);
		}
		return instance;
	}
}
