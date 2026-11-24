package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class V54__expand_pain_scales extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (var sql = connection.createStatement()) {
            for (String table : List.of("pain_scale_selections", "pain_records")) {
                // V45 used unnamed checks. PostgreSQL and H2 generate different names for them.
                for (String constraint : originalChecks(connection, table)) {
                    sql.execute("ALTER TABLE " + table + " DROP CONSTRAINT "
                            + sql.enquoteIdentifier(constraint, true));
                }
                sql.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + table + "_supported_scale"
                        + " CHECK (scale IN ('NUMERIC', 'WONG_BAKER', 'PAINAD', 'DOLOPLUS_2'))");
            }
            sql.execute("""
                    ALTER TABLE pain_records ADD CONSTRAINT pain_records_valid_score
                    CHECK (score BETWEEN 0 AND (CASE WHEN scale = 'DOLOPLUS_2' THEN 30 ELSE 10 END)
                        AND (scale <> 'WONG_BAKER' OR MOD(score, 2) = 0))
                    """);
        }
    }

    private List<String> originalChecks(Connection connection, String table) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT t.constraint_name
                FROM information_schema.table_constraints t
                JOIN information_schema.check_constraints c
                  ON c.constraint_schema = t.constraint_schema AND c.constraint_name = t.constraint_name
                WHERE t.table_schema = CURRENT_SCHEMA AND t.table_name = ?
                  AND t.constraint_type = 'CHECK' AND UPPER(c.check_clause) LIKE '%WONG_BAKER%'
                """)) {
            query.setString(1, table);
            try (var rows = query.executeQuery()) {
                List<String> names = new ArrayList<>();
                while (rows.next()) {
                    names.add(rows.getString(1));
                }
                return names;
            }
        }
    }
}
