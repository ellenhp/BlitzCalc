package com.blitzcalc;

public class KeypadState {
	String keypadText;
	boolean digitsEnabled;
	boolean operatorsEnabled;
	boolean lastCharIsOperator;
	
	public KeypadState(String keypadText, boolean digitsEnabled, boolean operatorsEnabled, boolean lastCharIsOperator) {
		super();
		this.keypadText = keypadText;
		this.digitsEnabled = digitsEnabled;
		this.operatorsEnabled = operatorsEnabled;
		this.lastCharIsOperator = lastCharIsOperator;
	}
	
	public String getKeypadText() {
		return keypadText;
	}
	public void setKeypadText(String keypadText) {
		this.keypadText = keypadText;
	}
	public boolean getDigitsEnabled() {
		return digitsEnabled;
	}
	public void setDigitsEnabled(boolean digitsEnabled) {
		this.digitsEnabled = digitsEnabled;
	}
	public boolean getOperatorsEnabled() {
		return operatorsEnabled;
	}
	public void setOperatorsEnabled(boolean operatorsEnabled) {
		this.operatorsEnabled = operatorsEnabled;
	}
	public boolean getLastCharIsOperator() {
		return lastCharIsOperator;
	}
	public void setLastCharIsOperator(boolean lastCharIsOperator) {
		this.lastCharIsOperator = lastCharIsOperator;
	}
}
