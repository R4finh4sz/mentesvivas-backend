package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.dto.link.DoctorSummaryResponse;
import com.mentesvivas.backend.dto.link.PartyResponse;

import java.util.List;

public record CareTeamResponse(
        List<DoctorSummaryResponse> doctors,
        List<PartyResponse> caregivers
) {
}
