package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.entity.user.MfaBackupCode;
import com.mentesvivas.backend.repository.user.MfaBackupCodeRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupCodeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private MfaBackupCodeRepository repository;

    private final TokenHasher hasher = new TokenHasher();
    private BackupCodeService service;

    @BeforeEach
    void setUp() {
        service = new BackupCodeService(repository, hasher);
    }

    @Test
    void regenerateReturnsTenDistinctTenDigitCodes() {
        List<String> codes = service.regenerate(USER_ID);

        assertThat(codes).hasSize(BackupCodeService.CODES_PER_BATCH).doesNotHaveDuplicates()
                .allMatch(code -> code.matches("\\d{10}"));
    }

    @Test
    void regenerateReplacesTheOldCodesBeforeSavingTheNewOnes() {
        service.regenerate(USER_ID);

        InOrder order = inOrder(repository);
        order.verify(repository).deleteAllByUserId(USER_ID);
        order.verify(repository).flush();
        order.verify(repository).saveAll(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void regenerateStoresOnlyHashesNeverThePlainCode() {
        List<String> codes = service.regenerate(USER_ID);

        ArgumentCaptor<List<MfaBackupCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(BackupCodeService.CODES_PER_BATCH)
                .allSatisfy(stored -> {
                    assertThat(stored.getCodeHash()).hasSize(64);
                    assertThat(codes).noneMatch(code -> stored.getCodeHash().contains(code));
                    assertThat(stored.getUserId()).isEqualTo(USER_ID);
                    assertThat(stored.isUsed()).isFalse();
                });
    }

    @Test
    void consumeMarksAnUnusedCodeAsUsed() {
        MfaBackupCode stored = new MfaBackupCode(USER_ID, "hash", Instant.now());
        when(repository.findByUserIdAndCodeHash(org.mockito.ArgumentMatchers.eq(USER_ID), anyString()))
                .thenReturn(Optional.of(stored));

        assertThat(service.consume(USER_ID, "1234567890")).isTrue();
        assertThat(stored.isUsed()).isTrue();
    }

    @Test
    void consumeRejectsACodeThatWasAlreadyUsed() {
        MfaBackupCode stored = new MfaBackupCode(USER_ID, "hash", Instant.now());
        stored.setUsedAt(Instant.now());
        when(repository.findByUserIdAndCodeHash(org.mockito.ArgumentMatchers.eq(USER_ID), anyString()))
                .thenReturn(Optional.of(stored));

        assertThat(service.consume(USER_ID, "1234567890")).isFalse();
    }

    @Test
    void consumeRejectsAnUnknownCode() {
        when(repository.findByUserIdAndCodeHash(org.mockito.ArgumentMatchers.eq(USER_ID), anyString()))
                .thenReturn(Optional.empty());

        assertThat(service.consume(USER_ID, "1234567890")).isFalse();
    }

    @Test
    void clearRemovesEveryCodeOfTheUser() {
        service.clear(USER_ID);

        verify(repository).deleteAllByUserId(USER_ID);
    }

    @Test
    void remainingCountsTheUnusedCodes() {
        when(repository.countByUserIdAndUsedAtIsNull(USER_ID)).thenReturn(7L);

        assertThat(service.remaining(USER_ID)).isEqualTo(7L);
    }

    @Test
    void recognizesTheBackupCodeFormat() {
        assertThat(BackupCodeService.looksLikeBackupCode("1234567890")).isTrue();
        assertThat(BackupCodeService.looksLikeBackupCode("123456")).isFalse();
        assertThat(BackupCodeService.looksLikeBackupCode("12345678ab")).isFalse();
        assertThat(BackupCodeService.looksLikeBackupCode(null)).isFalse();
    }
}
