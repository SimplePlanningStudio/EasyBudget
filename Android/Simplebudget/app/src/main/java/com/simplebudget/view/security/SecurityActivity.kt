package com.simplebudget.view.security

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simplebudget.R
import com.simplebudget.helper.interfaces.HashListener
import kotlinx.android.synthetic.main.dialog_security.*


class SecurityActivity : AppCompatActivity(), HashListener {

    private lateinit var requiredHash: String
    private var showTabIndex: Int = 0

    companion object {
        const val REQUEST_CODE_SECURITY_TYPE = "VERIFICATION_TYPE"
        const val VERIFICATION = "VERIFICATION"
        const val SET_PIN = "SET_PIN"
        private var SECURITY_TYPE = ""
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        this.window
            .setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

        setContentView(R.layout.dialog_security)

        requiredHash = intent.getStringExtra("HASH") ?: ""
        showTabIndex = intent.getIntExtra("TAB_INDEX", 0)
        SECURITY_TYPE = intent.getStringExtra(REQUEST_CODE_SECURITY_TYPE) ?: ""

        pin_lock_holder.initTab(requiredHash, this)

        ivClose.setOnClickListener { onCancelFail() }
    }

    /**
     *
     */
    private fun onCancelFail() {
        when (SECURITY_TYPE) {
            VERIFICATION -> {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            SET_PIN -> {
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra("HASH", pin_lock_holder.hash)
                )
                finish()
            }
        }
    }

    /**
     *
     */
    override fun receivedHash(hash: String, type: Int) {
        when (SECURITY_TYPE) {
            VERIFICATION -> {
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
            SET_PIN -> {
                setResult(Activity.RESULT_OK, Intent().putExtra("HASH", pin_lock_holder.hash))
                finish()
            }
        }
    }

    override fun error(errorMsg: String) {
        error.text = errorMsg
        error.visibility = View.VISIBLE
        shakeAnimation(error)
        object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                error.visibility = View.GONE
            }
        }.start()
    }

    /**
     *
     */
    override fun onBackPressed() {
        onCancelFail()
    }

    /**
     *
     */
    private fun shakeAnimation(view: TextView) {
        view.animation = AnimationUtils.loadAnimation(this, R.anim.shake_animation)
    }
}