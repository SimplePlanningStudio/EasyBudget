package com.simplebudget.helper.numpad;

import android.view.View;

import static com.simplebudget.helper.numpad.NumPadButton.CUSTOM_BUTTON_1;
import static com.simplebudget.helper.numpad.NumPadButton.CUSTOM_BUTTON_2;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_0;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_1;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_2;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_3;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_4;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_5;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_6;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_7;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_8;
import static com.simplebudget.helper.numpad.NumPadButton.NUM_9;

/**
 * Number pad click listener.
 */
public class NumPadClickListener implements View.OnClickListener {

    private static OnNumPadClickListener mListener;

    public NumPadClickListener(OnNumPadClickListener listener) {
        mListener = listener;
    }

    /**
     * Reads the clicked button tag and returns its respective {@link NumPadButton}.
     * Throws {@link NumPadListenerException} if listener was not set.
     */
    @Override
    public void onClick(View v) {
        if (mListener == null) {
            throw new NumPadListenerException("Number pad listener is not set");
        }
        mListener.onPadClicked(tagToNumPadButton((String) v.getTag()));
    }

    private NumPadButton tagToNumPadButton(String tag) {
        switch (tag) {
            case "0":
                return NUM_0;
            case "1":
                return NUM_1;
            case "2":
                return NUM_2;
            case "3":
                return NUM_3;
            case "4":
                return NUM_4;
            case "5":
                return NUM_5;
            case "6":
                return NUM_6;
            case "7":
                return NUM_7;
            case "8":
                return NUM_8;
            case "9":
                return NUM_9;
            case "custom1":
                return CUSTOM_BUTTON_1;
            default:
                return CUSTOM_BUTTON_2;
        }
    }
}
