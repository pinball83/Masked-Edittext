package com.github.pinball83.app

import android.os.Bundle
import com.github.pinball83.maskededittext.MaskedEditText


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val maskedEditText: MaskedEditText = this.findViewById(R.id.masked_edit_text)
        maskedEditText.setIconCallback(IconCallback { unmaskedText: String ->
            android.util.Log.d(
                TAG,
                "onIconPushed: $unmaskedText"
            )
            maskedEditText.setMaskedText("          ")
        })

        val maskedEditText1: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .icon(R.drawable.ic_account_circle)
                .format("[1][2][3] [4][5][6]-[7][8]-[10][9]") //we change format output text, swap last two digit
                .iconCallback(IconCallback { unmaskedText: String? ->
                    android.util.Log.d(TAG, "onIconPushed: ")
                    Toast.makeText(
                        this@MainActivity,
                        String.format("Unmasked formatted text %s", unmaskedText),
                        Toast.LENGTH_SHORT
                    ).show()
                })
                .build()
        maskedEditText1.setInputType(InputType.TYPE_CLASS_NUMBER)

        val editText1: MaskedEditText = com.github.pinball83.maskededittext.MaskedEditText.Builder(
            this
        )
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .icon(android.R.drawable.ic_menu_close_clear_cancel)
            .build()
        editText1.setInputType(InputType.TYPE_CLASS_NUMBER)
        editText1.setEnabled(false)
        editText1.setMaskedText("1234567891")

        val editText2: MaskedEditText = com.github.pinball83.maskededittext.MaskedEditText.Builder(
            this
        )
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER)
        editText2.setEnabled(false)
        editText2.setMaskedText("9263998787")

        val secondEditText: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("Q***************")
                .notMaskedSymbol("*")
                .build()

        val secondEditText1: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("**-****-*********")
                .notMaskedSymbol("*")
                .build()

        val thirdEditText: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("*****")
                .notMaskedSymbol("*")
                .build()
        thirdEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        val thirdEditText1: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("***4***")
                .notMaskedSymbol("*")
                .build()
        thirdEditText1.setMaskedText("888488")
        thirdEditText1.setInputType(InputType.TYPE_CLASS_NUMBER)

        val fordEditText: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            )
                .mask("TSH***************")
                .notMaskedSymbol("*")
                .build()
        fordEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        // check without mask
        val fordEditText1: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            ).build()
        fordEditText1.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val fordEditText2: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            ).build()
        fordEditText2.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val fordEditText3: MaskedEditText =
            com.github.pinball83.maskededittext.MaskedEditText.Builder(
                this
            ).build()
        fordEditText3.setInputType(InputType.TYPE_CLASS_NUMBER)

        val layout: LinearLayout = findViewById(R.id.container)
        layout.addView(maskedEditText1)
        layout.addView(editText1)
        layout.addView(editText2)
        layout.addView(secondEditText)
        layout.addView(secondEditText1)
        layout.addView(thirdEditText)
        layout.addView(thirdEditText1)
        layout.addView(fordEditText)
        layout.addView(fordEditText1)
        layout.addView(fordEditText2)
        layout.addView(fordEditText3)
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}
