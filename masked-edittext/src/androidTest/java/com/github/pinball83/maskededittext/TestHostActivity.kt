package com.github.pinball83.maskededittext

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

class TestHostActivity : AppCompatActivity() {
    lateinit var masked: MaskedEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(root)

        masked = MaskedEditText(this).apply {
            contentDescription = "masked-under-test"
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(masked)
    }
}

