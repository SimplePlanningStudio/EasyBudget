package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_PIN
import com.simplemobiletools.commons.helpers.numpad.NumPadViewPhase3
import com.simplemobiletools.commons.helpers.toast
import com.simplemobiletools.commons.interfaces.HashListener
import com.simplemobiletools.commons.interfaces.SecurityTab
import kotlinx.android.synthetic.main.tab_pin.view.*
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), SecurityTab {
    private var hash = ""
    private var requiredHash = ""
    private var pin = ""
    lateinit var hashListener: HashListener

    override fun onFinishInflate() {
        super.onFinishInflate()

        val customNumberPad: NumPadViewPhase3 = findViewById(R.id.custom_number_pad)
        customNumberPad.setNumberPadClickListener { button ->
            if (button.name == "NUM_1") addNumber("1")
            if (button.name == "NUM_2") addNumber("2")
            if (button.name == "NUM_3") addNumber("3")
            if (button.name == "NUM_4") addNumber("4")
            if (button.name == "NUM_5") addNumber("5")
            if (button.name == "NUM_6") addNumber("6")
            if (button.name == "NUM_7") addNumber("7")
            if (button.name == "NUM_8") addNumber("8")
            if (button.name == "NUM_9") addNumber("9")
            if (button.name == "NUM_0") addNumber("0")
            // Done button
            if (button.name == "CUSTOM_BUTTON_2") confirmPIN()
            // Clear button
            if (button.name == "CUSTOM_BUTTON_1") clear()
        }
    }

    override fun initTab(requiredHash: String, listener: HashListener, scrollView: MyScrollView) {
        this.requiredHash = requiredHash
        hash = requiredHash
        hashListener = listener
    }

    private fun addNumber(number: String) {
        if (pin.length < 10) {
            pin += number
            updatePinCode()
        }
        performHapticFeedback()
    }

    private fun clear() {
        if (pin.isNotEmpty()) {
            pin = pin.substring(0, pin.length - 1)
            updatePinCode()
        }
        performHapticFeedback()
    }

    private fun confirmPIN() {
        val newHash = getHashedPin()
        if (pin.isEmpty()) {
            context.toast(R.string.please_enter_pin)
        } else if (hash.isEmpty()) {
            hash = newHash
            resetPin()
            pin_lock_title.setText(R.string.repeat_pin)
        } else if (hash == newHash) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        } else {
            resetPin()
            context.toast(R.string.wrong_pin)
            if (requiredHash.isEmpty()) {
                hash = ""
                pin_lock_title.setText(R.string.enter_pin)
            }
        }
        performHapticFeedback()
    }

    private fun resetPin() {
        pin = ""
        pin_lock_current_pin.text = ""
    }

    private fun updatePinCode() {
        pin_lock_current_pin.text = "*".repeat(pin.length)
        if (hash.isNotEmpty() && hash == getHashedPin()) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        }
    }

    private fun getHashedPin(): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(pin.toByteArray(charset("UTF-8")))
        val digest = messageDigest.digest()
        val bigInteger = BigInteger(1, digest)
        return String.format(Locale.getDefault(), "%0${digest.size * 2}x", bigInteger).toLowerCase(
            Locale.getDefault()
        )
    }

    override fun visibilityChanged(isVisible: Boolean) {}
}
