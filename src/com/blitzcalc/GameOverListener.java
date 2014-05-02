package com.blitzcalc;

public interface GameOverListener {
	public void onNewGame();
	public void onDisplayScoreboards(int goal);
	public void onRequestSubmitScore(int goal, double time);
}
