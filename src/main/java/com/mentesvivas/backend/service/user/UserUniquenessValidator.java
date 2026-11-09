package com.mentesvivas.backend.service.user;

import com.mentesvivas.backend.exception.user.CpfAlreadyRegisteredException;
import com.mentesvivas.backend.exception.user.EmailAlreadyRegisteredException;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUniquenessValidator {

    private final UserRepository userRepository;

    public void validate(String email, String cpf) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new CpfAlreadyRegisteredException();
        }
    }
}
