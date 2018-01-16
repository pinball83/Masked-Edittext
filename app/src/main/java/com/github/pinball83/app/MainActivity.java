package com.github.pinball83.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.pinball83.maskededittext.MaskedEditText;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final MaskedEditText maskedEditText = this.findViewById(R.id.masked_edit_text);
        maskedEditText.setIconCallback(new MaskedEditText.IconCallback() {
            @Override
            public void onIconPushed(String unmaskedText) {
                Log.d(TAG, "onIconPushed: " + unmaskedText);
                maskedEditText.setMaskedText("          ");
            }
        });

        final MaskedEditText maskedEditText1 = new MaskedEditText.Builder(this)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .icon(R.drawable.ic_account_circle)
                .format("[1][2][3] [4][5][6]-[7][8]-[10][9]")//we change format output text, swap last two digit
                .iconCallback(new MaskedEditText.IconCallback() {
                    @Override
                    public void onIconPushed(String unmaskedText) {
                        Log.d(TAG, "onIconPushed: ");
                        Toast.makeText(MainActivity.this, String.format("Unmasked formatted text %s", unmaskedText), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        maskedEditText1.setInputType(InputType.TYPE_CLASS_NUMBER);

        MaskedEditText editText1 = new MaskedEditText.Builder(this)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .icon(android.R.drawable.ic_menu_close_clear_cancel)
                .build();
        editText1.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText1.setEnabled(false);
        editText1.setMaskedText("1234567891");

        MaskedEditText editText2 = new MaskedEditText.Builder(this)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText2.setEnabled(false);
        editText2.setMaskedText("9263998787");

        MaskedEditText secondEditText = new MaskedEditText.Builder(this)
                .mask("Q***************")
                .notMaskedSymbol("*")
                .build();

        MaskedEditText secondEditText1 = new MaskedEditText.Builder(this)
                .mask("**-****-*********")
                .notMaskedSymbol("*")
                .build();

        MaskedEditText thirdEditText = new MaskedEditText.Builder(this)
                .mask("*****")
                .notMaskedSymbol("*")
                .build();
        thirdEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        MaskedEditText thirdEditText1 = new MaskedEditText.Builder(this)
                .mask("***4***")
                .notMaskedSymbol("*")
                .build();
        thirdEditText1.setMaskedText("888488");
        thirdEditText1.setInputType(InputType.TYPE_CLASS_NUMBER);

        MaskedEditText fordEditText = new MaskedEditText.Builder(this)
                .mask("TSH***************")
                .notMaskedSymbol("*")
                .build();
        fordEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        // check without mask
        MaskedEditText fordEditText1 = new MaskedEditText.Builder(this).build();
        fordEditText1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        MaskedEditText fordEditText2 = new MaskedEditText.Builder(this).build();
        fordEditText2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        MaskedEditText fordEditText3 = new MaskedEditText.Builder(this).build();
        fordEditText3.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = findViewById(R.id.container);
        layout.addView(maskedEditText1);
        layout.addView(editText1);
        layout.addView(editText2);
        layout.addView(secondEditText);
        layout.addView(secondEditText1);
        layout.addView(thirdEditText);
        layout.addView(thirdEditText1);
        layout.addView(fordEditText);
        layout.addView(fordEditText1);
        layout.addView(fordEditText2);
        layout.addView(fordEditText3);
    }
}
