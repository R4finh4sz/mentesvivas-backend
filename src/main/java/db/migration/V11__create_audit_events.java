package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__create_audit_events extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var database = connection.getMetaData().getDatabaseProductName();
        if (!database.equals("PostgreSQL") && !database.equals("H2")) {
            throw new IllegalStateException("A auditoria suporta PostgreSQL e H2 de testes.");
        }
        try (var sql = connection.createStatement()) {
            sql.execute("""
                    CREATE TABLE audit_events (
                        id UUID PRIMARY KEY,
                        occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        event_type VARCHAR(50) NOT NULL,
                        actor_user_id UUID,
                        patient_user_id UUID,
                        on_behalf_of_user_id UUID,
                        corrects_event_id UUID,
                        justification VARCHAR(500),
                        before_value VARCHAR(1000),
                        after_value VARCHAR(1000)
                    )
                    """);
            sql.execute("CREATE INDEX idx_audit_events_time ON audit_events (occurred_at)");
            sql.execute("CREATE INDEX idx_audit_events_actor ON audit_events (actor_user_id, occurred_at)");
            sql.execute("CREATE INDEX idx_audit_events_patient ON audit_events (patient_user_id, occurred_at)");
            if (database.equals("PostgreSQL")) {
                sql.execute("""
                        CREATE FUNCTION reject_audit_mutation() RETURNS trigger
                        LANGUAGE plpgsql AS $$
                        BEGIN
                            RAISE EXCEPTION 'Audit logs are append-only' USING ERRCODE = '42501';
                        END;
                        $$
                        """);
                sql.execute("CREATE TRIGGER prevent_audit_mutation BEFORE UPDATE OR DELETE OR TRUNCATE ON audit_events "
                        + "FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_mutation()");
                sql.execute("ALTER TABLE audit_events ENABLE ALWAYS TRIGGER prevent_audit_mutation");
                sql.execute("REVOKE UPDATE, DELETE, TRUNCATE ON audit_events FROM PUBLIC");
            }
        }
    }
}
