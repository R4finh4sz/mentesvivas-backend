package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/caregivers/me/caregiver-moment")
@RequiredArgsConstructor
public class CaregiverMomentController {

    private final CurrentUser currentUser;

    @GetMapping
    public Content content() {
        requireCaregiver();
        return new Content(
                List.of(
                        new Tip("Pausa breve", "Quando houver uma pausa segura, sente-se e solte os ombros."),
                        new Tip("Atenção ao presente", "Observe três coisas ao seu redor e respire no seu ritmo.")),
                List.of(new Tip("Organize o espaço", "Deixe os materiais de uso frequente ao seu alcance."),
                        new Tip("Alterne a posição", "Mude de posição durante as pausas sempre que puder.")));
    }

    @GetMapping("/exhaustion")
    public Support exhaustion() {
        requireCaregiver();
        return new Support("CVV — Centro de Valorização da Vida", "188",
                "Este módulo oferece apoio ao cuidador. Para atendimento clínico ou de emergência, "
                        + "procure o serviço adequado.");
    }

    private void requireCaregiver() {
        if (currentUser.get().getRole() != Role.CAREGIVER) {
            throw new AccessDeniedException("Acesso restrito a cuidadores.");
        }
    }

    public record Content(List<Tip> relaxation, List<Tip> ergonomics) {}

    public record Tip(String title, String text) {}

    public record Support(String name, String phone, String notice) {}
}
