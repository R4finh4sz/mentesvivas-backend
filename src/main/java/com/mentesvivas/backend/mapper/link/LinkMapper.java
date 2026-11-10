package com.mentesvivas.backend.mapper.link;

import com.mentesvivas.backend.dto.link.CaregiverLinkResponse;
import com.mentesvivas.backend.dto.link.DoctorLinkResponse;
import com.mentesvivas.backend.dto.link.DoctorSummaryResponse;
import com.mentesvivas.backend.dto.link.PartyResponse;
import com.mentesvivas.backend.dto.notification.NotificationResponse;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.notification.Notification;
import com.mentesvivas.backend.entity.user.User;

public final class LinkMapper {

    private LinkMapper() {
    }

    public static PartyResponse toParty(User user) {
        return new PartyResponse(user.getId(), user.getFullName(), user.getPhotoUrl());
    }

    public static DoctorSummaryResponse toDoctor(User doctor) {
        return new DoctorSummaryResponse(doctor.getId(), doctor.getFullName(), doctor.getCrm(),
                doctor.getCrmState(), doctor.getPhotoUrl());
    }

    public static CaregiverLinkResponse toResponse(CaregiverPatientLink link, User caregiver, User patient) {
        return new CaregiverLinkResponse(link.getId(), link.getStatus(), toParty(caregiver), toParty(patient),
                link.getCreatedAt());
    }

    public static DoctorLinkResponse toResponse(DoctorPatientLink link, User doctor, User patient) {
        return new DoctorLinkResponse(link.getId(), link.getStatus(), toDoctor(doctor), toParty(patient),
                link.isDoctorConfirmed(), link.isPatientAuthorized(), link.getCreatedAt());
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getReferenceId(),
                notification.getMessage(), notification.getCreatedAt(), notification.getReadAt() != null);
    }
}
