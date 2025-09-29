package com.github.pinball83.maskededittext;

import android.content.Context;
import android.text.SpannableStringBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for MaskedInputFilter functionality
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MaskedInputFilterTest {

    private Context context;
    private MaskedEditText maskedEditText;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
    }

    @Test
    public void testInputFilterWithPhoneMask() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        // Simulate typing "1" at the beginning
        CharSequence result = maskedEditText.getFilters()[0].filter(
                "1", 0, 1, 
                maskedEditText.getText(), 2, 2
        );
        
        assertNotNull(result);
    }

    @Test
    public void testInputFilterWithDeletion() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        maskedEditText.setMaskedText("1234567890");
        
        // Simulate deletion
        CharSequence result = maskedEditText.getFilters()[0].filter(
                "", 0, 0, 
                maskedEditText.getText(), 2, 3
        );
        
        assertNotNull(result);
    }

    @Test
    public void testInputFilterWithInvalidCharacter() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        // Test with SpannableStringBuilder (should return source unchanged)
        SpannableStringBuilder source = new SpannableStringBuilder("test");
        CharSequence result = maskedEditText.getFilters()[0].filter(
                source, 0, 4, 
                maskedEditText.getText(), 0, 0
        );
        
        assertEquals(source, result);
    }

    @Test
    public void testInputFilterAtMaskBoundary() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("***-***")
                .notMaskedSymbol("*")
                .build();

        // Test input at the end of mask
        CharSequence result = maskedEditText.getFilters()[0].filter(
                "7", 0, 1, 
                maskedEditText.getText(), 7, 7
        );
        
        assertNotNull(result);
    }

    @Test
    public void testInputFilterWithMultipleCharacters() {
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();

        // Test pasting multiple characters
        CharSequence result = maskedEditText.getFilters()[0].filter(
                "123", 0, 3, 
                maskedEditText.getText(), 2, 2
        );
        
        assertNotNull(result);
    }
}