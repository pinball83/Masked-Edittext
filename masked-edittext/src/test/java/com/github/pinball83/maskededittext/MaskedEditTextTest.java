package com.github.pinball83.maskededittext;

import android.content.Context;
import android.text.InputType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MaskedEditText functionality
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MaskedEditTextTest {

    private Context context;
    private MaskedEditText maskedEditText;

    @Mock
    private MaskedEditText.IconCallback mockIconCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;
    }

    @Test
    public void testBasicMaskCreation() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        assertNotNull(maskedEditText);
        assertEquals("8 (   )    --  ", maskedEditText.getText().toString());
    }

    @Test
    public void testPhoneNumberMask() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("1234567890");
        assertEquals("8 (123) 456 78-90", maskedEditText.getText().toString());
        assertEquals("1234567890", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testCreditCardMask() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("**** **** **** ****")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("1234567890123456");
        assertEquals("1234 5678 9012 3456", maskedEditText.getText().toString());
        assertEquals("1234567890123456", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testCustomFormat() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .format("[1][2][3] [4][5][6]-[7][8]-[10][9]")
                .build();

        maskedEditText.setMaskedText("1234567890");
        assertEquals("123 456-78-09", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testPartialInput() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("12345");
        assertEquals("8 (123) 45 --  ", maskedEditText.getText().toString());
        assertEquals("12345     ", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testEmptyInput() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("");
        assertEquals("8 (   )    --  ", maskedEditText.getText().toString());
        assertEquals("          ", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testInputTypeNumber() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        // Verify that input type is set correctly
        assertTrue((maskedEditText.getInputType() & InputType.TYPE_CLASS_NUMBER) != 0);
    }

    @Test
    public void testIconCallback() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .iconCallback(mockIconCallback)
                .build();

        maskedEditText.setMaskedText("1234567890");
        
        // Simulate icon click - this would normally be triggered by touch events
        maskedEditText.setIconCallback(mockIconCallback);
        assertNotNull(maskedEditText);
    }

    @Test
    public void testBuilderPattern() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("***-***-****")
                .notMaskedSymbol("*")
                .format("[1][2][3]-[4][5][6]-[7][8][9][10]")
                .build();

        assertNotNull(maskedEditText);
        maskedEditText.setMaskedText("1234567890");
        assertEquals("123-456-7890", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testNoMaskBuilder() {
        maskedEditText = new MaskedEditText.Builder(context).build();
        assertNotNull(maskedEditText);
        
        maskedEditText.setText("test input");
        assertEquals("test input", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testRequiredField() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setRequired(true);
        assertTrue(maskedEditText.isRequired());

        maskedEditText.setRequired(false);
        assertFalse(maskedEditText.isRequired());
    }

    @Test
    public void testLongInput() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("***-***")
                .notMaskedSymbol("*")
                .build();

        // Input longer than mask should be truncated
        maskedEditText.setMaskedText("1234567890");
        assertEquals("123-456", maskedEditText.getText().toString());
        assertEquals("123456", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testSpecialCharacterMask() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("Q***************")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("ABCDEFGHIJKLMNO");
        assertEquals("QABCDEFGHIJKLMNO", maskedEditText.getText().toString());
        assertEquals("ABCDEFGHIJKLMNO", maskedEditText.getUnmaskedText());
    }

    @Test
    public void testMixedMask() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("TSH***************")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("123456789012345");
        assertEquals("TSH123456789012345", maskedEditText.getText().toString());
        assertEquals("123456789012345", maskedEditText.getUnmaskedText());
    }
}