package com.mentesvivas.backend.service.game;

import org.springframework.stereotype.Component;

@Component
public class BingoFeedback {

    public String hit(String instrument) {
        return "Muito bem! Este e o som de " + instrument + ".";
    }

    public String retry() {
        return "Quase la! Vamos ouvir de novo e tentar mais uma vez?";
    }

    public String completion() {
        return "Parabens! Voce concluiu o Bingo Musical. Que tal escolher outro jogo?";
    }

    public String narration() {
        return "Que instrumento voce esta ouvindo?";
    }
}
