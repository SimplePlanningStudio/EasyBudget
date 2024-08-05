/*
 *   Copyright 2024 Waheed Nazir
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.simplebudget.helper.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplebudget.R
import com.simplebudget.helper.PROTECTION_PIN
import com.simplebudget.helper.extensions.performHapticFeedback
import com.simplebudget.helper.interfaces.HashListener
import com.simplebudget.helper.numpad.NumPadView
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    var hash = ""
    private var requiredHash = ""
    private var pin = ""
    lateinit var hashListener: HashListener
    lateinit var pinLockTitle: MyTextView
    lateinit var pinLockCurrentPin: MyTextView

    override fun onFinishInflate() {
        super.onFinishInflate()

        pinLockTitle = findViewById(R.id.pin_lock_title)
        pinLockCurrentPin = findViewById(R.id.pin_lock_current_pin)

        val customNumberPad: NumPadView = findViewById(R.id.custom_number_pad)
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

    fun initTab(requiredHash: String, listener: HashListener) {
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
            hashListener.error(context.resources.getString(R.string.please_enter_pin))
        } else if (hash.isEmpty()) {
            hash = newHash
            resetPin()
            pinLockTitle.setText(R.string.repeat_pin)
        } else if (hash == newHash) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        } else {
            resetPin()
            hashListener.error(context.resources.getString(R.string.wrong_pin))
            if (requiredHash.isEmpty()) {
                hash = ""
                pinLockTitle.setText(R.string.enter_pin)
            }
        }
        performHapticFeedback()
    }

    private fun resetPin() {
        pin = ""
        pinLockCurrentPin.text = ""
    }

    private fun updatePinCode() {
        pinLockCurrentPin.text = "◉".repeat(pin.length)
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

}
