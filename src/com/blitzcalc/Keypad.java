package com.blitzcalc;

import java.util.HashMap;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.congrace.exp4j.ExpressionBuilder;

public class Keypad extends Fragment {

	private long lastValue = 0;
	private boolean empty = true;
	private boolean digitsAllowed = true;
	private boolean operatorsAllowed = false;

	private HashMap<String, Button> buttons = new HashMap<String, Button>();
	private Stack<KeypadState> stack = new Stack<KeypadState>();
	private boolean lastCharIsOperator = false;

	private TextView output;

	private View myView = null;

	private KeypadListener listener;

	Timer colorTimer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_keypad, container, false);

		myView = v;

		output = (TextView) v.findViewById(R.id.resultDisplay);
		final RelativeLayout buttonHolder = (RelativeLayout) myView.findViewById(R.id.button_holder);
		buttonHolder.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				// I don't know of a functional difference between these, but still good to avoid deprecated stuff I guess?
				if (Build.VERSION.SDK_INT >= 16) {
					buttonHolder.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				} else {
					buttonHolder.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				LayoutBuilder builder = new LayoutBuilder();
				builder.buildLayout(buttonHolder);
			}
		});

		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("lastValue", lastValue);
		outState.putBoolean("empty", empty);
		outState.putBoolean("numberAllowed", digitsAllowed);
		outState.putString("expression", output.getText().toString());
		outState.putBoolean("ellipsize_end", output.getEllipsize() == TruncateAt.END);
	}

	public void clearAll() {
		lastValue = 0;
		if (output != null) {
			output.setText("0");
		}
		stack.clear();
		empty = true;
		setNumberAllowed(true);
		setOperatorAllowed(false);
	}

	private void backspace() {
		if (stack.isEmpty()) {
			clearAll();
		} else {
			KeypadState state = stack.pop();
			output.setText(state.getKeypadText());
			setNumberAllowed(state.getDigitsEnabled());
			setOperatorAllowed(state.getOperatorsEnabled());
			lastCharIsOperator = state.getLastCharIsOperator();
		}
	}

	private void pushState() {
		stack.push(new KeypadState(output.getText().toString(), digitsAllowed, operatorsAllowed, lastCharIsOperator));
	}

	private void evaluate() {
		if (!empty) {
			String textToEvaluate = output.getText().toString();
			textToEvaluate = textToEvaluate.replace(getString(R.string.multiply_operator).charAt(0), '*');
			textToEvaluate = textToEvaluate.replace(getString(R.string.divide_operator).charAt(0), '/');
			Log.d("math", "Evaluating expression: " + textToEvaluate);
			ExpressionBuilder builder = new ExpressionBuilder(textToEvaluate);
			try {
				lastValue = Math.round(builder.build().calculate());
				output.setText(Long.toString(lastValue));
				setNumberAllowed(false);
				setOperatorAllowed(true);
				output.setEllipsize(TruncateAt.END);
				stack.clear();
				if (listener != null) {
					listener.onNewResult(lastValue);
				}
			} catch (Exception ex) {
				if (colorTimer != null) {
					colorTimer.cancel();
				}
				colorTimer = new Timer();
				colorTimer.schedule(new TimerTask() {
					int redVal = 255;

					@Override
					public void run() {
						Keypad.this.getActivity().runOnUiThread(new ColorSetter(Color.argb(255, redVal, 0, 0), output));
						redVal -= 2;
						if (redVal < 0) {
							cancel();
						}
					}
				}, 1, 17);
			}
		}
	}

	private void addOperator(String operator) {
		if (!empty) {
			if (lastCharIsOperator) {
				backspace();
			}
			pushState();
			output.setText(output.getText().toString() + operator);
			setNumberAllowed(true);
			setOperatorAllowed(true);
			lastCharIsOperator = true;
		}
		output.setEllipsize(TruncateAt.START);
	}

	private void onClickNumber(int digit) {
		if (!digitsAllowed) {
			return;
		}
		if (empty) {
			empty = false;
			output.setText(Integer.toString(digit));
		} else {
			pushState();
			output.setText(output.getText().toString() + digit);
		}
		setNumberAllowed(false);
		setOperatorAllowed(true);
		lastCharIsOperator = false;
		output.setEllipsize(TruncateAt.START);
	}

	public double getValue() {
		return lastValue;
	}

	private void setNumberAllowed(boolean allowed) {
		if (myView == null) {
			return;
		}
		digitsAllowed = allowed;
		for (int i = 1; i < 10; i++) {
			buttons.get("" + i).setEnabled(digitsAllowed);
		}
	}

	private void setOperatorAllowed(boolean allowed) {
		if (myView == null) {
			return;
		}
		operatorsAllowed = allowed;
		buttons.get("+").setEnabled(allowed);
		buttons.get("-").setEnabled(allowed);
		buttons.get(getString(R.string.multiply_operator)).setEnabled(allowed);
		buttons.get(getString(R.string.divide_operator)).setEnabled(allowed);
		buttons.get("^").setEnabled(allowed);
	}

	public void setKeypadListener(KeypadListener l) {
		listener = l;
	}

	private class LayoutBuilder {

		int clickEvent = MotionEvent.ACTION_UP;

		private void leftParenPushed(View v) {
			if (!empty) {
				pushState();
				output.setText(output.getText().toString() + ((Button) v).getText().toString());
				setNumberAllowed(true);
				setOperatorAllowed(false);
			} else {
				output.setText(((Button) v).getText().toString());
				empty = false;
			}
			lastCharIsOperator = false;
		}

		private void rightParenPushed(View v) {
			if (!empty) {
				pushState();
				output.setText(output.getText().toString() + ((Button) v).getText().toString());
				setNumberAllowed(false);
				setOperatorAllowed(true);
			} else {
				output.setText(((Button) v).getText().toString());
				empty = false;
			}
			lastCharIsOperator = false;
		}

		private void click(View v) {
			if (PreferenceManager.getDefaultSharedPreferences(Keypad.this.getActivity()).getBoolean("haptic", true)) {
				v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
			}
		}

		private OnTouchListener digitListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					onClickNumber(Integer.parseInt(((Button) v).getText().toString()));
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private OnTouchListener operatorListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					addOperator(((Button) v).getText().toString());
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private OnTouchListener leftParenListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					leftParenPushed(v);
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private OnTouchListener rightParenListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					rightParenPushed(v);
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private OnTouchListener equalsListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					evaluate();
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private OnTouchListener backspaceListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == clickEvent) {
					backspace();
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					click(v);
				}
				return false;
			}
		};

		private String[] text = { "(", ")", "^", getString(R.string.backspace_text), "7", "8", "9", getString(R.string.divide_operator), "4", "5", "6", getString(R.string.multiply_operator), "1",
				"2", "3", "-", "=", "+" };
		private OnTouchListener[] handlers = { leftParenListener, rightParenListener, operatorListener, backspaceListener, digitListener, digitListener, digitListener, operatorListener,
				digitListener, digitListener, digitListener, operatorListener, digitListener, digitListener, digitListener, operatorListener, equalsListener, operatorListener };

		private Button addButton(RelativeLayout base, int index, OnTouchListener handler, int row, int col) {
			int btnIndex = row * 4 + col;
			int btnId = btnIndex + 1;
			int prevLeft = btnId - 1;
			int prevTop = btnId - 4;
			Button btn = new Button(getActivity());
			btn.setText(text[index]);
			btn.setOnTouchListener(handlers[index]);
			btn.setId(btnId);
			btn.setWidth(base.getWidth() / 4);
			btn.setHeight(base.getHeight() / 5);
			btn.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
			btn.setPadding(0, 0, 0, 0);
			btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.button_text_size));

			Log.d("size", "" + getResources().getDimension(R.dimen.button_text_size));

			buttons.put(text[index], btn);

			RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if (col == 0) {
				relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			} else {
				relativeParams.addRule(RelativeLayout.RIGHT_OF, prevLeft);
			}
			if (row == 0) {
				relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			} else {
				relativeParams.addRule(RelativeLayout.BELOW, prevTop);
			}

			base.addView(btn, relativeParams);
			return btn;
		}

		public void buildLayout(RelativeLayout base) {
			for (int row = 0; row < 4; row++) {
				for (int col = 0; col < 4; col++) {
					addButton(base, row * 4 + col, null, row, col);
				}
			}

			Button btn = addButton(base, 16, null, 4, 0);
			btn.setWidth(3 * base.getWidth() / 4);
			addButton(base, 17, null, 4, 1);

			clearAll();
		}
	}

	private class ColorSetter implements Runnable {
		int color;
		TextView view;

		public ColorSetter(int color, TextView view) {
			this.view = view;
			this.color = color;
		}

		public void run() {
			view.setTextColor(color);
		}
	}
}
