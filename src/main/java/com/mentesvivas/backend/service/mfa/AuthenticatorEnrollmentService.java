package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.dto.mfa.AuthenticatorActivatedResponse;
import com.mentesvivas.backend.dto.mfa.AuthenticatorSetupResponse;
import com.mentesvivas.backend.dto.mfa.BackupCodesResponse;
import com.mentesvivas.backend.dto.mfa.MfaStatusResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticatorEnrollmentService {

    private static final Duration SETUP_VALIDITY = Duration.ofMinutes(10);

    private final CurrentUser currentUser;
    private final TotpService totpService;
    private final SecretCipher cipher;
    private final TemporaryValue pendingSetups;
    private final AuthenticatorCodeVerifier verifier;
    private final AuthenticatorCredentialVerifier credentialVerifier;
    private final BackupCodeService backupCodes;
    private final AuditTrail auditTrail;

    @Transactional(readOnly = true)
    public MfaStatusResponse status() {
        return new MfaStatusResponse(currentUser.get().getMfaMethod(), List.of(MfaMethod.values()));
    }

    @Transactional
    public AuthenticatorSetupResponse begin() {
        User user = currentUser.get();
        if (user.getMfaMethod() == MfaMethod.AUTHENTICATOR) {
            throw new ConflictException("O aplicativo autenticador ja esta ativo. Desative-o antes de configurar outro.");
        }
        String secret = totpService.generateSecret();
        pendingSetups.store(setupKey(user), cipher.encrypt(secret), SETUP_VALIDITY);
        return new AuthenticatorSetupResponse(secret, totpService.provisioningUri(user.getEmail(), secret));
    }

    @Transactional
    public AuthenticatorActivatedResponse confirm(String code) {
        User user = currentUser.get();
        String encrypted = pendingSetups.get(setupKey(user))
                .orElseThrow(() -> new ConflictException("Inicie a configuracao do aplicativo autenticador primeiro."));
        verifier.verify(user.getId(), cipher.decrypt(encrypted), code);
        user.setTotpSecret(encrypted);
        user.setMfaMethod(MfaMethod.AUTHENTICATOR);
        pendingSetups.remove(setupKey(user));
        auditTrail.record(AuditRecord.of(AuditEventType.AUTHENTICATOR_ENABLED, user.getId())
                .withValues(MfaMethod.EMAIL.name(), MfaMethod.AUTHENTICATOR.name()));
        return new AuthenticatorActivatedResponse(user.getMfaMethod(), List.of(MfaMethod.values()),
                backupCodes.regenerate(user.getId()));
    }

    @Transactional
    public MfaStatusResponse disable(String code) {
        User user = currentUser.get();
        if (user.getMfaMethod() != MfaMethod.AUTHENTICATOR || user.getTotpSecret() == null) {
            throw new ConflictException("O aplicativo autenticador nao esta ativo.");
        }
        credentialVerifier.verify(user, code);
        user.setTotpSecret(null);
        user.setMfaMethod(MfaMethod.EMAIL);
        backupCodes.clear(user.getId());
        auditTrail.record(AuditRecord.of(AuditEventType.AUTHENTICATOR_DISABLED, user.getId())
                .withValues(MfaMethod.AUTHENTICATOR.name(), MfaMethod.EMAIL.name()));
        return new MfaStatusResponse(user.getMfaMethod(), List.of(MfaMethod.values()));
    }

    @Transactional
    public BackupCodesResponse regenerateBackupCodes(String code) {
        User user = currentUser.get();
        if (user.getMfaMethod() != MfaMethod.AUTHENTICATOR || user.getTotpSecret() == null) {
            throw new ConflictException("O aplicativo autenticador nao esta ativo.");
        }
        credentialVerifier.verify(user, code);
        BackupCodesResponse response = new BackupCodesResponse(backupCodes.regenerate(user.getId()));
        auditTrail.record(AuditRecord.of(AuditEventType.BACKUP_CODES_REGENERATED, user.getId()));
        return response;
    }

    private String setupKey(User user) {
        return "mfa:setup:" + user.getId();
    }
}
