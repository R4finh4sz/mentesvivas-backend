package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.AnswerRecognitionRequest;
import com.mentesvivas.backend.dto.game.RecognitionAnswerResponse;
import com.mentesvivas.backend.dto.game.RecognitionQuestionResponse;
import com.mentesvivas.backend.entity.game.FamilyMember;
import com.mentesvivas.backend.entity.game.RecognitionQuestion;
import com.mentesvivas.backend.entity.game.RecognitionSession;
import com.mentesvivas.backend.entity.patient.ConsentScope;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.game.FamilyMemberRepository;
import com.mentesvivas.backend.repository.game.RecognitionSessionRepository;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import com.mentesvivas.backend.service.patient.PatientConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecognitionGameService {

    private static final String COMPLETION = "Parabéns! Você concluiu o jogo. Volte à lista de jogos.";

    private final PatientCareAccess access;
    private final PatientConsentService consent;
    private final FamilyMemberRepository members;
    private final RecognitionSessionRepository sessions;
    private final RecognitionGamePolicy policy;
    private final Clock clock;

    @Transactional
    public RecognitionQuestionResponse start(UUID patientId) {
        User starter = access.requireWriter(patientId);
        access.requireGamesAllowed(starter);
        consent.require(patientId, ConsentScope.FAMILY_PHOTOS);
        Map<UUID, FamilyMember> gallery = gallery(patientId);
        if (!policy.isUnlocked(gallery.size())) {
            throw new ConflictException(policy.guidance(gallery.size()));
        }

        boolean advanced = sessions.existsByPatientIdAndFinishedAtIsNotNull(patientId);
        List<FamilyMember> people = new ArrayList<>(gallery.values());
        Collections.shuffle(people);
        Instant now = clock.instant();
        RecognitionSession session = new RecognitionSession();
        session.setPatientId(patientId);
        session.setStartedBy(starter.getId());
        session.setStartedAt(now);
        session.setQuestionShownAt(now);
        for (FamilyMember target : people) {
            session.getQuestions().add(buildQuestion(target, people, advanced));
        }
        return view(sessions.save(session), gallery);
    }

    @Transactional
    public RecognitionQuestionResponse current(UUID patientId, UUID sessionId) {
        return view(openSession(patientId, sessionId), gallery(patientId));
    }

    @Transactional
    public RecognitionAnswerResponse answer(UUID patientId, UUID sessionId, AnswerRecognitionRequest request) {
        RecognitionSession session = openSession(patientId, sessionId);
        if (request.position() != session.getQuestionIndex() + 1) {
            throw new ConflictException("Esta pergunta não é a atual.");
        }
        RecognitionQuestion question = session.getQuestions().get(session.getQuestionIndex());
        if (!question.options().contains(request.memberId())) {
            throw new ImpossibleValueException("Escolha uma das opções da pergunta.");
        }

        Map<UUID, FamilyMember> gallery = gallery(patientId);
        RecognitionQuestionResponse current = view(session, gallery);
        if (!question.getMemberId().equals(request.memberId())) {
            question.recordError();
            return new RecognitionAnswerResponse(false, "Vamos tentar mais uma vez?", null, false, current);
        }

        Instant now = clock.instant();
        question.recordAnswer(Math.max(0, Duration.between(session.getQuestionShownAt(), now).toMillis()));
        String audio = consent.granted(patientId, ConsentScope.FAMILY_AUDIO)
                ? gallery.get(question.getMemberId()).getAudioUrl() : null;
        int next = session.getQuestionIndex() + 1;
        if (next == session.getQuestions().size()) {
            session.setFinishedAt(now);
            return new RecognitionAnswerResponse(true, COMPLETION, audio, true, null);
        }
        session.setQuestionIndex(next);
        session.setQuestionShownAt(now);
        return new RecognitionAnswerResponse(true, "Muito bem!", audio, false, view(session, gallery));
    }

    private RecognitionSession openSession(UUID patientId, UUID sessionId) {
        access.requireGamesAllowed(access.requireWriter(patientId));
        consent.require(patientId, ConsentScope.FAMILY_PHOTOS);
        RecognitionSession session = sessions.findByIdAndPatientId(sessionId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado."));
        if (session.getFinishedAt() != null) {
            throw new ConflictException(COMPLETION);
        }
        return session;
    }

    private Map<UUID, FamilyMember> gallery(UUID patientId) {
        return members.findByPatientIdOrderByNameAsc(patientId).stream()
                .collect(Collectors.toMap(FamilyMember::getId, Function.identity()));
    }

    private RecognitionQuestion buildQuestion(FamilyMember target, List<FamilyMember> people, boolean advanced) {
        List<FamilyMember> distractors = new ArrayList<>(people);
        distractors.remove(target);
        Collections.shuffle(distractors);
        distractors.sort(Comparator.comparing(person ->
                target.getKinship().equalsIgnoreCase(person.getKinship()) != advanced));
        List<UUID> options = new ArrayList<>();
        options.add(target.getId());
        distractors.stream().limit(RecognitionGamePolicy.OPTIONS_PER_QUESTION - 1L)
                .map(FamilyMember::getId).forEach(options::add);
        Collections.shuffle(options);
        return new RecognitionQuestion(target.getId(), options);
    }

    private RecognitionQuestionResponse view(RecognitionSession session, Map<UUID, FamilyMember> gallery) {
        RecognitionQuestion question = session.getQuestions().get(session.getQuestionIndex());
        List<UUID> optionIds = question.options();
        if (!gallery.keySet().containsAll(optionIds)) {
            throw new ConflictException("O cadastro de familiares mudou. Inicie um novo jogo.");
        }
        List<RecognitionQuestionResponse.Option> options = optionIds.stream()
                .map(gallery::get)
                .map(person -> new RecognitionQuestionResponse.Option(person.getId(), person.getName(), person.getKinship()))
                .toList();
        return new RecognitionQuestionResponse(session.getId(), session.getQuestionIndex() + 1,
                session.getQuestions().size(), "Quem é esta pessoa?", gallery.get(question.getMemberId()).getPhotoUrl(),
                options);
    }
}
