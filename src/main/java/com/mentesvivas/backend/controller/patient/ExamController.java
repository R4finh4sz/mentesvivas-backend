package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.ExamFileContent;
import com.mentesvivas.backend.dto.patient.ExamResponse;
import com.mentesvivas.backend.dto.patient.UploadExamRequest;
import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.service.patient.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse upload(@PathVariable UUID patientId, @Valid @ModelAttribute UploadExamRequest body,
                               @RequestPart("file") MultipartFile file) {
        try {
            return service.upload(patientId, body, file.getOriginalFilename(), file.getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @GetMapping("/{examId}/file")
    public ResponseEntity<byte[]> download(@PathVariable UUID patientId, @PathVariable UUID examId) {
        ExamFileContent file = service.download(patientId, examId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }

    @DeleteMapping("/{examId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID patientId, @PathVariable UUID examId) {
        service.delete(patientId, examId);
    }
}
