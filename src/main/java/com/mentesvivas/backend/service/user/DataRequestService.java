package com.mentesvivas.backend.service.user;

import com.mentesvivas.backend.dto.user.DataRequestResponse;
import com.mentesvivas.backend.dto.user.DataRequestResponse.Status;
import com.mentesvivas.backend.dto.user.DataRequestResponse.Type;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataRequestService {

    private static final String SELECT = "select id, requested_by, request_type, status, opened_at, updated_at from data_requests";
    private static final String PAGE = " order by opened_at desc, id desc limit :size offset :offset";

    private final JdbcClient jdbc;
    private final CurrentUser currentUser;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public DataRequestResponse open(Type type) {
        UUID actor = currentUser.get().getId();
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        jdbc.sql("""
                insert into data_requests (id, requested_by, request_type, status, opened_at, updated_at)
                values (:id, :actor, :type, 'OPEN', :date, :date)
                """).param("id", id).param("actor", actor).param("type", type.name())
                .param("date", now.atOffset(ZoneOffset.UTC)).update();
        audit.record(AuditRecord.of(AuditEventType.DATA_REQUEST_OPENED, actor)
                .withValues(null, id + ":" + type));
        return new DataRequestResponse(id, actor, type, Status.OPEN, now, now);
    }

    @Transactional(readOnly = true)
    public List<DataRequestResponse> mine(int page, int size) {
        return jdbc.sql(SELECT + " where requested_by = :actor" + PAGE)
                .param("actor", currentUser.get().getId()).param("size", size).param("offset", (long) page * size)
                .query(DataRequestService::map).list();
    }

    @Transactional(readOnly = true)
    public List<DataRequestResponse> queue(Status status, int page, int size) {
        var query = status == null ? jdbc.sql(SELECT + PAGE)
                : jdbc.sql(SELECT + " where status = :status" + PAGE).param("status", status.name());
        return query.param("size", size).param("offset", (long) page * size).query(DataRequestService::map).list();
    }

    @Transactional
    public DataRequestResponse update(UUID id, Status target) {
        DataRequestResponse previous = jdbc.sql(SELECT + " where id = :id for update").param("id", id)
                .query(DataRequestService::map).optional()
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
        if (previous.status() == target) {
            return previous;
        }
        boolean valid = switch (previous.status()) {
            case OPEN -> target == Status.IN_REVIEW;
            case IN_REVIEW -> target == Status.RESOLVED || target == Status.DECLINED;
            case RESOLVED, DECLINED -> false;
        };
        if (!valid) {
            throw new ConflictException("Transição de status da solicitação inválida.");
        }

        Instant now = clock.instant();
        jdbc.sql("update data_requests set status = :status, updated_at = :date where id = :id")
                .param("id", id).param("status", target.name()).param("date", now.atOffset(ZoneOffset.UTC)).update();
        audit.record(AuditRecord.of(AuditEventType.DATA_REQUEST_STATUS_CHANGED, currentUser.get().getId())
                .withValues(id + ":" + previous.status(), id + ":" + target));
        return new DataRequestResponse(id, previous.requestedBy(), previous.type(), target, previous.openedAt(), now);
    }

    private static DataRequestResponse map(ResultSet row, int index) throws SQLException {
        return new DataRequestResponse(row.getObject("id", UUID.class), row.getObject("requested_by", UUID.class),
                Type.valueOf(row.getString("request_type")), Status.valueOf(row.getString("status")),
                row.getObject("opened_at", OffsetDateTime.class).toInstant(),
                row.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
