/*
 * Copyright (C) 2016 Gustavo Fão Valvassori
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pinball83.maskededittext.saripaar;

import com.mobsandgeeks.saripaar.ContextualAnnotationRule;
import com.mobsandgeeks.saripaar.ValidationContext;

public class MaskedEditTextRule extends ContextualAnnotationRule<MaskedRequire, String> {

    protected MaskedEditTextRule(final ValidationContext validationContext, final MaskedRequire cpfCnpj) {
        super(validationContext, cpfCnpj);
    }

    @Override
    public boolean isValid(String text) {
        boolean isValidAnnotation =
                !mRuleAnnotation.value().isEmpty() &&
                !mRuleAnnotation.notMaskedSymbol().isEmpty();

        try {
            if (isValidAnnotation) {
                String emptyField = mRuleAnnotation.value().replaceAll(
                        mRuleAnnotation.notMaskedSymbol(), mRuleAnnotation.replacementChar()
                );

                boolean emptyTest = !emptyField.trim().equals(text.trim());

                if (mRuleAnnotation.complete()) {
                    int maskLength = mRuleAnnotation.value().length();
                    for (int i = 0; i < maskLength; i++) {
                        String maskChar = mRuleAnnotation.value().substring(i, i + 1);
                        if ("*".equals(maskChar)) {
                            String maskedChar = text.substring(i , i + 1);
                            if (mRuleAnnotation.replacementChar().equals(maskedChar)) {
                                return false;
                            }
                        }
                    }
                }

                return emptyTest;
            }
        } catch (Exception ex) {}

        return false;
    }
}
