package com.agentplatform.common.core.security.stub;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlainCredentialStoreTest {

    private final PlainCredentialStore store = new PlainCredentialStore();

    @Test
    void encryptDecryptIdentity() {
        assertThat(store.encrypt("Bearer secret")).isEqualTo("Bearer secret");
        assertThat(store.decrypt("Bearer secret")).isEqualTo("Bearer secret");
    }

    @Test
    void maskShortValues() {
        assertThat(store.mask(null)).isNull();
        assertThat(store.mask("")).isEmpty();
        assertThat(store.mask("ab")).isEqualTo("**");
        assertThat(store.mask("abcd")).isEqualTo("****");
        assertThat(store.mask("abcde")).isEqualTo("****cde");
    }

    @Test
    void maskMatchesDoDExamples() {
        // 任务 1.8 DoD: mask("sk-abc123xyz") -> "****xyz"
        assertThat(store.mask("sk-abc123xyz")).isEqualTo("****xyz");
        assertThat(store.mask("Bearer secret")).isEqualTo("****ret");
    }
}
