package com.example.paymentrouting.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchIdsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "A | A",
            "' a ' | A",
            "ab-1 | AB-1",
            "x9 | X9"
    })
    void givenUsableInput_whenNormalising_thenTrimmedAndUpperCased(String raw, String expected) {
        // when - then
        assertThat(BranchIds.normalise(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A B", "A,B", "A_B", "ABCDEFGHIJKLMNOPQRSTU", "\u00C9",
            "\u00DF", "stra\u00DFe", "\u0131", "\u017F", "\uFB01"})
    void givenUnusableInput_whenNormalising_thenThrowsInvalidBranchException(String raw) {
        // when - then
        assertThatThrownBy(() -> BranchIds.normalise(raw))
                .isInstanceOf(InvalidBranchException.class);
    }
}
