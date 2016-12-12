package com.github.pinball83.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.github.pinball83.maskededittext.MaskedEditText;
import com.github.pinball83.maskededittext.saripaar.MaskedRequire;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @MaskedRequire(value = "8 (***) *** **-**", complete = true, notMaskedSymbol = "\\*", replacementChar = "")
    MaskedEditText maskedEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Validator.registerAnnotation(MaskedRequire.class);
        final Validator validator = new Validator(this);
        validator.setValidationListener(new Validator.ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                Toast.makeText(MainActivity.this, "Validation success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onValidationFailed(List<ValidationError> errors) {
                for (ValidationError error : errors) {
                    Toast.makeText(MainActivity.this, error.getCollatedErrorMessage(MainActivity.this), Toast.LENGTH_SHORT).show();
                }
            }
        });

        maskedEditText = (MaskedEditText) this.findViewById(R.id.masked_edit_text);
        maskedEditText.setMaskIconCallback(new MaskedEditText.MaskIconCallback() {
            @Override
            public void onIconPushed() {
                validator.validate();
            }
        });

        final MaskedEditText maskedEditText1 = new MaskedEditText(this,
                "8 (***) *** **-**",
                "*",
                getResources().getDrawable(R.drawable.ic_account_circle));

        maskedEditText1.setInputType(InputType.TYPE_CLASS_NUMBER);
        maskedEditText1.setMaskIconCallback(new MaskedEditText.MaskIconCallback() {
            @Override
            public void onIconPushed() {
                System.out.println("Icon pushed");
                if (maskedEditText1.isEmpty()) {
                    Toast.makeText(MainActivity.this, "It is empty", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Unmasked text " + maskedEditText1.getUnmaskedText(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        MaskedEditText editText1 = new MaskedEditText(this, "8 (***) *** **-**", "*");
        editText1.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText1.setEnabled(false);
        editText1.setMaskedText("1234567891");
        editText1.setFormat("[1][2][3] [4][5][6]-[7][8]-[10][9]");
//        final String format = editText1.getUnmaskedText();
//        System.out.println("format = " + format);

        MaskedEditText editText2 = new MaskedEditText(this, "8 (***) *** **-**", "*");
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText2.setEnabled(false);
        editText2.setMaskedText("9263998787");

        MaskedEditText secondEditText = new MaskedEditText(this, "Q***************", "*");

        MaskedEditText secondEditText1 = new MaskedEditText(this, "**-****-*********", "*");

        MaskedEditText thirdEditText = new MaskedEditText(this, "*****", "*");
        thirdEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        MaskedEditText thirdEditText1 = new MaskedEditText(this, "***4***", "*");
        thirdEditText1.setMaskedText("888488");
        thirdEditText1.setInputType(InputType.TYPE_CLASS_NUMBER);

        MaskedEditText fordEditText = new MaskedEditText(this, "TSH***************", "*");
        fordEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        // check without mask
        MaskedEditText fordEditText1 = new MaskedEditText(this);
        fordEditText1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        MaskedEditText fordEditText2 = new MaskedEditText(this);
        fordEditText2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        MaskedEditText fordEditText3 = new MaskedEditText(this);
        fordEditText3.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
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
