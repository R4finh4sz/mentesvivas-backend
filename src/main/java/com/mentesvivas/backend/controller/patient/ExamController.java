package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.ExamResponse;
import com.mentesvivas.backend.dto.patient.UploadExamRequest;
import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.service.patient.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService service;

    @GetMapping
    public List<ExamResponse> list(@PathVariable UUID patientId,
                                   @RequestParam(required = false) ExamCategory category) {
        return service.list(patientId, category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse upload(@PathVariable UUID patientId, @Valid @RequestBody UploadExamRequest body) {
        return service.upload(patientId, body);
    }

    @DeleteMapping("/{examId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID patientId, @PathVariable UUID examId) {
        service.delete(patientId, examId);
    }
}
