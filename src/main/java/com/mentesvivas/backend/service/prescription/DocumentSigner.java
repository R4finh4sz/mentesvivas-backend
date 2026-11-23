package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.user.User;

public interface DocumentSigner {

    SignatureResult sign(byte[] content, User signer);
}
