package com.mentesvivas.backend.service.mfa;

public interface SecretCipher {

    String encrypt(String plain);

    String decrypt(String encrypted);
}
