package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V12__create_access_events extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var database = connection.getMetaData().getDatabaseProductName();
        if (!database.equals("PostgreSQL") && !database.equals("H2")) {
            throw new IllegalStateException("O registro de acessos suporta PostgreSQL e H2 de testes.");
        }
        try (var sql = connection.createStatement()) {
            sql.execute("""
                    CREATE TABLE access_events (
                        id UUID PRIMARY KEY,
                        occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        accessor_user_id UUID NOT NULL,
                        patient_user_id UUID NOT NULL,
                        area VARCHAR(30) NOT NULL
                    )
                    """);
            sql.execute("CREATE INDEX idx_access_events_patient ON access_events (patient_user_id, occurred_at)");
            if (database.equals("PostgreSQL")) {
                sql.execute("CREATE TRIGGER prevent_access_mutation BEFORE UPDATE OR DELETE OR TRUNCATE ON access_events "
                        + "FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_mutation()");
                sql.execute("ALTER TABLE access_events ENABLE ALWAYS TRIGGER prevent_access_mutation");
                sql.execute("REVOKE UPDATE, DELETE, TRUNCATE ON access_events FROM PUBLIC");
            }
        }
    }
}
