package com.example.demo_app

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.demo_app.databinding.ActivityMainBinding
import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.InputStateMachine
import com.github.pinball83.maskededittext.MaskedEditText

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPhoneSample()
        setupCardSample()
        setupDynamicSample(binding.dynamicHost)
    }

    private fun setupPhoneSample() {
        binding.phoneField.apply {
            setMaskedText("9261234567")
            setStateChangeListener(logState(binding.phoneState))
        }
    }

    private fun setupCardSample() {
        binding.cardField.apply {
            setMaskIcon(android.R.drawable.ic_menu_send)
            setFormat("[1][2][3][4]-[5][6][7][8]-[9][10][11][12]-[13][14][15][16]")
            setStateChangeListener(logState(binding.cardState))
            setIconCallback { value ->
                Toast.makeText(context, getString(R.string.icon_toast, value), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDynamicSample(container: FrameLayout) {
        val dynamic = MaskedEditText.Builder(this)
            .mask("**-****-******")
            .notMaskedSymbol("*")
            .required(true)
            .icon(android.R.drawable.ic_menu_view)
            .iconCallback { value ->
                Toast.makeText(this, getString(R.string.icon_toast, value), Toast.LENGTH_SHORT).show()
            }
            .stateChangeListener(logState(null))
            .build().apply {
                hint = "AA-1234-XXXXXX"
            }

        container.addView(dynamic)
        dynamic.setMaskedText("AB1234567890")
    }

    private fun logState(target: TextView?): InputStateMachine.InputStateListener {
        return InputStateMachine.InputStateListener { _, newState, event ->
            val formatted = when (newState) {
                InputState.EMPTY -> getString(R.string.state_template, "empty", event.name)
                InputState.PARTIAL -> getString(R.string.state_template, "partial", event.name)
                InputState.COMPLETE -> getString(R.string.state_template, "complete", event.name)
                InputState.INVALID -> getString(R.string.state_template, "invalid", event.name)
                InputState.FOCUSED -> getString(R.string.state_template, "focused", event.name)
                InputState.UNFOCUSED -> getString(R.string.state_template, "unfocused", event.name)
            }
            target?.text = formatted
        }
    }
}
