package com.mentesvivas.backend.controller.user;

import com.mentesvivas.backend.dto.user.DataRequestResponse;
import com.mentesvivas.backend.dto.user.DataRequestResponse.Status;
import com.mentesvivas.backend.dto.user.DataRequestResponse.Type;
import com.mentesvivas.backend.service.user.DataRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DataRequestController {

    private final DataRequestService service;

    @PostMapping("/api/users/me/data-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public DataRequestResponse open(@Valid @RequestBody OpenRequest body) {
        return service.open(body.type());
    }

    @GetMapping("/api/users/me/data-requests")
    public List<DataRequestResponse> mine(@RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.mine(page, size);
    }

    @GetMapping("/api/admin/data-requests")
    public List<DataRequestResponse> queue(@RequestParam(required = false) Status status,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.queue(status, page, size);
    }

    @PutMapping("/api/admin/data-requests/{id}/status")
    public DataRequestResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest body) {
        return service.update(id, body.status());
    }

    public record OpenRequest(@NotNull Type type) {}

    public record UpdateRequest(@NotNull Status status) {}
}
