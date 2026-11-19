package com.mentesvivas.backend.controller.admin;

import com.mentesvivas.backend.dto.admin.AccountStatusRequest;
import com.mentesvivas.backend.dto.admin.AdminUserPageResponse;
import com.mentesvivas.backend.dto.admin.AdminUserResponse;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public AdminUserPageResponse list(@RequestParam(required = false) Role role,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.list(role, page, size);
    }

    @PostMapping("/{id}/deactivate")
    public AdminUserResponse deactivate(@PathVariable UUID id, @Valid @RequestBody AccountStatusRequest body) {
        return service.deactivate(id, body.justification());
    }

    @PostMapping("/{id}/reactivate")
    public AdminUserResponse reactivate(@PathVariable UUID id, @Valid @RequestBody AccountStatusRequest body) {
        return service.reactivate(id, body.justification());
    }
}
