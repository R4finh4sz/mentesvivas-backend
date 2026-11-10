package com.mentesvivas.backend.controller.link;

import com.mentesvivas.backend.dto.link.DoctorSummaryResponse;
import com.mentesvivas.backend.service.link.DoctorDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor-directory")
@RequiredArgsConstructor
public class DoctorDirectoryController {

    private final DoctorDirectoryService service;

    @GetMapping
    public List<DoctorSummaryResponse> search(@RequestParam(name = "q", required = false) String query) {
        return service.search(query);
    }
}
