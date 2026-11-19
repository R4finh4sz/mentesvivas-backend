package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V14__single_administrator extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var database = connection.getMetaData().getDatabaseProductName();
        if (database.equals("PostgreSQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE UNIQUE INDEX uk_single_administrator ON users (role) WHERE role = 'ADMINISTRATOR'");
            }
        }
    }
}
