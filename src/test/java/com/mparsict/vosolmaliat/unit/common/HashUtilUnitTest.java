package com.mparsict.vosolmaliat.unit.common;

import com.mparsict.vosolmaliat.common.util.HashUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilUnitTest {

    @Test
    void sha256IsDeterministicAndReturnsHexDigest() {
        String first = HashUtil.sha256("debt-paper-123");
        String second = HashUtil.sha256("debt-paper-123");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
        assertThat(first).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256ChangesWhenInputChanges() {
        assertThat(HashUtil.sha256("debt-paper-123"))
                .isNotEqualTo(HashUtil.sha256("debt-paper-124"));
    }
}
