package com.simplemobiletools.commons.helpers.numpad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplemobiletools.commons.R;


/**
 * Draw the number pad view.
 */
public class NumPadViewPhase3 extends RelativeLayout {

    private Button mButton0, mButton1, mButton2, mButton3, mButton4, mButton5, mButton6,
            mButton7, mButton8, mButton9;
    private TextView mCustomButton1, mCustomButton2;

    public NumPadViewPhase3(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpView(context);
    }

    public void setNumberPadClickListener(OnNumPadClickListener onNumberPadClickListener) {
        setUpPadButtons(new NumPadClickListener(onNumberPadClickListener));
    }

    private void setUpView(Context context) {
        View view = inflate(context, R.layout.view_number_pad_phase3, this);
        initButtons(view);
    }

    private void initButtons(View view) {
        mButton0 = view.findViewById(R.id.button0);
        mButton1 = view.findViewById(R.id.button1);
        mButton2 = view.findViewById(R.id.button2);
        mButton3 = view.findViewById(R.id.button3);
        mButton4 = view.findViewById(R.id.button4);
        mButton5 = view.findViewById(R.id.button5);
        mButton6 = view.findViewById(R.id.button6);
        mButton7 = view.findViewById(R.id.button7);
        mButton8 = view.findViewById(R.id.button8);
        mButton9 = view.findViewById(R.id.button9);
        mCustomButton1 = view.findViewById(R.id.button_custom1);
        mCustomButton2 = view.findViewById(R.id.button_custom2);
    }

    private void setUpPadButtons(NumPadClickListener numberPadClickListener) {
        mButton0.setOnClickListener(numberPadClickListener);
        mButton1.setOnClickListener(numberPadClickListener);
        mButton2.setOnClickListener(numberPadClickListener);
        mButton3.setOnClickListener(numberPadClickListener);
        mButton4.setOnClickListener(numberPadClickListener);
        mButton5.setOnClickListener(numberPadClickListener);
        mButton6.setOnClickListener(numberPadClickListener);
        mButton7.setOnClickListener(numberPadClickListener);
        mButton8.setOnClickListener(numberPadClickListener);
        mButton9.setOnClickListener(numberPadClickListener);
        mCustomButton1.setOnClickListener(numberPadClickListener);
        mCustomButton2.setOnClickListener(numberPadClickListener);
    }
}
