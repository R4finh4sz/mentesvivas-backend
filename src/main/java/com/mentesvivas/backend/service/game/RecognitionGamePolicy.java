package com.mentesvivas.backend.service.game;

import org.springframework.stereotype.Component;

@Component
public class RecognitionGamePolicy {

    public static final int OPTIONS_PER_QUESTION = 4;

    public int minimumPeople() {
        return OPTIONS_PER_QUESTION;
    }

    public boolean isUnlocked(long registeredPeople) {
        return registeredPeople >= minimumPeople();
    }

    public String guidance(long registeredPeople) {
        if (isUnlocked(registeredPeople)) {
            return null;
        }
        long missing = minimumPeople() - registeredPeople;
        return "Cadastre mais " + missing + " pessoa(s) para liberar o jogo de reconhecimento.";
    }
}
