package app.backend.entities;

import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;


public class Session {
    private Connection connection;
    private Stack<Cancel> savepoints;
    private ConnectionInfo connectionInfo;
    private Statement saveStatement;
    private DatabaseMetaData meta;
    private boolean supportsSchema;

    public Session(ConnectionInfo info) {
        try {
            connection = connect(info);
        } catch (SQLException e) {
            throw new RuntimeException("Problems with connection: " + e.getMessage());
        }
    }

    public Connection connect(ConnectionInfo info) throws SQLException {
        Connection connection = null;
        this.connectionInfo = info;
        switch (connectionInfo.getConnectionType()) {
            case SQLITE -> {
                SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
                sqLiteDataSource.setUrl(connectionInfo.getProperties().get("url"));
                connection = sqLiteDataSource.getConnection();
            }
            case POSTGRESQL -> {
                PGSimpleDataSource pgSimpleDataSource = new PGSimpleDataSource();
                Map<String, String> props = connectionInfo.getProperties();
                pgSimpleDataSource.setUser(props.get("accessToken"));
                pgSimpleDataSource.setPassword("31032003");
                pgSimpleDataSource.setServerNames(new String[]{props.get("host")});
                pgSimpleDataSource.setPortNumbers(new int[]{Integer.parseInt(props.get("port"))});
                pgSimpleDataSource.setDatabaseName(props.get("datasourceId"));
                pgSimpleDataSource.setSslmode("disable");
                // Если будут проблемы убрать
                pgSimpleDataSource.setTcpKeepAlive(true);
                pgSimpleDataSource.setTcpNoDelay(true);

                connection = pgSimpleDataSource.getConnection();
            }
            default -> throw new RuntimeException("Unknown connection type: " + connectionInfo.getConnectionType());
        }
        connection.setAutoCommit(false);
        saveStatement = connection.createStatement(); // часть Амины
        savepoints = new Stack<>();
        meta = connection.getMetaData();
        supportsSchema = meta.supportsSchemasInDataManipulation();
        return connection;
    }

    // Connection functions
    public void reconnect(ConnectionInfo info) {
        try {
            disconnect();
            connection = connect(info);
        } catch (SQLException e) {
            throw new RuntimeException("Problems with connection: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Problems with disconnection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isSupportsSchema() {
        return supportsSchema;
    }

    // Statements functions
    // Это использовать для заранее подготовленных запросов (колонки и таблицы должны быть определены)
    public PreparedStatement getPreparedStatement(String sql) {
        try {
            switch (connectionInfo.getConnectionType()) {
                case SQLITE -> {
                    return connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                }
                case POSTGRESQL -> {
                    return connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                }
                default -> {
                    return connection.prepareStatement(sql);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't get prepared statement: " + e.getMessage());
        }
    }

    // execute query: get data, update data, update schema

    // Для запросов пользователя
    public Statement getStatement() {
        try {
            switch (connectionInfo.getConnectionType()) {
                case SQLITE -> {
                    return connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                }
                case POSTGRESQL -> {
                    return connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                }
                default -> {
                    return connection.createStatement();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't get statement: " + e.getMessage());
        }
    }

    public DataTable getDataFromTable(String tableName, int rowsToGet) {
        try {
            String sql = "SELECT * FROM " + tableName;
            Statement statement = getStatement();
            List<List<String>> rows = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();
            int rowsGot = 0;

            long startTime = System.currentTimeMillis();
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int columnsNumber = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnsNumber; i++) {
                columnNames.add(resultSetMetaData.getColumnName(i));
            }
            while (rowsGot < rowsToGet && rs.next()) {
                rowsGot++;
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            long executionTime = System.currentTimeMillis() - startTime;

            return new DataTable(columnNames, rows, rs, rowsGot, executionTime);
        } catch (SQLException e) {
            throw new RuntimeException("Can't execute query to get data: " + e.getMessage());
        }
    }

    public void insertData(String tableName, List<String> newValues, List<String> columnNames) {
        String columns = columnNames.stream().reduce("", (x, y) -> x + ", " + y).substring(2);
        String values = newValues.stream().reduce("", (x, y) -> x + "\'" + ", " + "\'" + y).substring(3) + "\'";
        String sql = "INSERT INTO " + tableName + " (" + columns + ") " + "VALUES (" + values + ");";
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, tableName, Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Can't insert data: " + e.getMessage());
        }
    }

    public void deleteData(String tableName, DataTable dataTable, int index) {
        List<Key> keys = getKeys(tableName);
        List<String> keyColumns = new ArrayList<>();
        for (Key k : keys) {
            keyColumns.addAll(k.getColumns());
        }
        List<String> allColumns = dataTable.getColumnNames();

        String actualKey = allColumns.stream().filter(keyColumns::contains).findFirst().orElse(null);
        int colIndex = allColumns.indexOf(actualKey);
        String id = dataTable.getRows().get(index).get(colIndex);

        String sql = "DELETE FROM " + tableName + " WHERE " + actualKey + " = " + "\'" + id + "\'" + ";";
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, tableName, Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Can't delete data: " + e.getMessage());
        }

        dataTable.deleteRow(index);
    }

    private List<Integer> getColumnTypes(String tableName, List<Integer> columnNumbers) {
        List<Integer> columnTypes = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " LIMIT 1";
        try (Statement stmt = getStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int col : columnNumbers) {
                columnTypes.add(metaData.getColumnType(col + 1)); // ResultSetMetaData columns are 1-based
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving column types", e);
        }
        return columnTypes;
    }

    public void updateData(String tableName, DataTable dataTable, int rowNumber, List<Integer> columnNumbers, List<String> values) {
        List<Key> keys = getKeys(tableName);
        List<String> keyColumns = new ArrayList<>();
        for (Key k : keys) {
            keyColumns.addAll(k.getColumns());
        }
        List<String> allColumns = dataTable.getColumnNames();

        String actualKey = allColumns.stream().filter(keyColumns::contains).findFirst().orElse(null);
        int colIndex = allColumns.indexOf(actualKey);
        if (colIndex == -1) {
            colIndex = columnNumbers.get(columnNumbers.size() - 1);
        }
        if (actualKey == null) {
            actualKey = allColumns.get(colIndex);
        }
        String id = dataTable.getRows().get(rowNumber).get(colIndex);

        List<Integer> l = new ArrayList<>(columnNumbers);
        l.add(colIndex);
        List<Integer> columnTypes = getColumnTypes(tableName, l);
        String sql = "UPDATE " + tableName + " SET ";

        switch (connectionInfo.getConnectionType()) {
            case SQLITE, POSTGRESQL -> {
                for (int col : columnNumbers) {
                    sql += allColumns.get(col) + " = ?, ";
                }
                sql = sql.substring(0, sql.length() - 2) + " WHERE " + actualKey + " = ?;";
            }
            default ->
                    throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
        }

        PreparedStatement preparedStatement = getPreparedStatement(sql);
        try {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i), columnTypes.get(i));
            }
            preparedStatement.setObject(values.size() + 1, id, columnTypes.get(columnTypes.size() - 1));

            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, tableName, Cancel.CancelType.TABLE, rowNumber));
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Can't update data: " + e.getMessage());
        }

        dataTable.changeRow(rowNumber, columnNumbers, values);
    }

    public void createIndex(String indexName, String tableName, boolean isUnique, List<String> columnsNames) {
        String sql = "CREATE ";
        if (isUnique) {
            sql += "UNIQUE ";
        }
        sql += "INDEX IF NOT EXISTS " + indexName + " ON " + tableName;
        String columns = columnsNames.stream().reduce("", (x, y) -> x + ", " + y).substring(2);
        sql += " (" + columns + ");";

        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, indexName, Cancel.CancelType.INDEX, 0));
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create index: " + e.getMessage());
        }
//        updateSaveStatement(sql);
    }

    public void deleteIndex(String indexName) {
        String sql = "DROP INDEX IF EXISTS " + indexName + ";";
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, "", Cancel.CancelType.DELETED, 0));
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Can't drop index: " + e.getMessage());
        }
//        updateSaveStatement(sql);
    }

    public void createView(String viewName, String sql) {
        String query;
        switch (connectionInfo.getConnectionType()) {
            case SQLITE -> query = "CREATE VIEW IF NOT EXISTS " + viewName + " AS " + sql + ";";
            case POSTGRESQL -> query = "CREATE OR REPLACE VIEW " + viewName + " AS " + sql + ";";
            default ->
                    throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
        }
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, viewName, Cancel.CancelType.VIEW, 0));
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Can't drop index: " + e.getMessage());
        }
//        updateSaveStatement(query);
    }

    public void deleteView(String viewName) {
        String sql = "DROP VIEW IF EXISTS " + viewName + ";";
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, "", Cancel.CancelType.DELETED, 0));
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Can't drop index: " + e.getMessage());
        }
//        updateSaveStatement(sql);
    }

    public void updateSaveStatement(String sql) {
        try {
            saveStatement.addBatch(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Can't add new batch (save changes): " + e.getMessage());
        }
    }

    public void saveChanges() {
        try {
//            saveStatement.executeBatch();
            connection.commit();
            savepoints.clear();
        } catch (SQLException e) {
            throw new RuntimeException("Can't save changes: " + e.getMessage());
        }
    }

    public Cancel discardChanges() {
        try {
//            saveStatement.clearBatch();
            if (savepoints.isEmpty() || savepoints.size() == 1) {
                connection.rollback();
                return null;
            }
            connection.rollback(savepoints.peek().getSavepoint());
            return savepoints.pop();
        } catch (SQLException e) {
            throw new RuntimeException("Can't discard changes: " + e.getMessage());
        }
    }

    // Working with meta functions

    // PGSQL
    public List<Database> getDatabases() {
        try {
            List<Database> databaseList = new ArrayList<>();
            ResultSet resultSet = meta.getCatalogs();
            while (resultSet.next()) {
                databaseList.add(new Database(resultSet.getString("TABLE_CAT")));
            }
            return databaseList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // PGSQL
    // Не используем так как в данной реализации используем только схему public
    @Deprecated
    public List<Schema> getSchemas(String databaseName) {
        try {
            List<Schema> schemaList = new ArrayList<>();
            ResultSet resultSet = meta.getSchemas(databaseName, null);
            while (resultSet.next()) {
                schemaList.add(new Schema(resultSet.getString("TABLE_SCHEM")));
            }
            return schemaList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<View> getViews() {
        try {
            List<View> viewList = new ArrayList<>();
            Statement statement = getStatement();
            String query;
            switch (connectionInfo.getConnectionType()) {
                case POSTGRESQL ->
                        query = "SELECT table_name AS name, view_definition AS sql FROM information_schema.views WHERE table_schema='public';";
                case SQLITE -> query = "SELECT name, sql FROM sqlite_master WHERE type == \"view\"";
                default ->
                        throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
            }
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                viewList.add(new View(resultSet.getString("name"), resultSet.getString("sql")));
            }
            statement.close();
            return viewList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Table> getTables() {
        try {
            List<Table> tableList = new ArrayList<>();
            Statement statement = getStatement();
            String query;
            switch (connectionInfo.getConnectionType()) {
                case POSTGRESQL -> {
                    String queryCreateFunction = "CREATE OR REPLACE FUNCTION pg_get_tabledef(tablename text) RETURNS text AS $$\n" +
                            "DECLARE\n" +
                            "    result text;\n" +
                            "    col_result text;\n" +
                            "    con_result text;\n" +
                            "BEGIN\n" +
                            "    SELECT string_agg(\n" +
                            "                   '    ' || quote_ident(attname) || ' ' || format_type(atttypid, atttypmod) ||\n" +
                            "                   CASE WHEN NOT attnotnull THEN ' NULL' ELSE '' END ||\n" +
                            "                   CASE WHEN atthasdef THEN ' DEFAULT ' || pg_get_expr(adbin, adrelid) ELSE '' END,\n" +
                            "                   E',\\n'\n" +
                            "           )\n" +
                            "    INTO col_result\n" +
                            "    FROM pg_attribute\n" +
                            "             LEFT JOIN pg_attrdef ON pg_attribute.attnum = pg_attrdef.adnum AND pg_attribute.attrelid = pg_attrdef.adrelid\n" +
                            "    WHERE pg_attribute.attrelid = tablename::regclass\n" +
                            "      AND pg_attribute.attnum > 0\n" +
                            "      AND NOT pg_attribute.attisdropped;\n" +
                            "    SELECT string_agg(\n" +
                            "                   '    ' || pg_get_constraintdef(pg_constraint.oid), E',\\n'\n" +
                            "           )\n" +
                            "    INTO con_result\n" +
                            "    FROM pg_constraint\n" +
                            "    WHERE conrelid = tablename::regclass;\n" +
                            "    result := 'CREATE TABLE ' || quote_ident(tablename) || E' (\\n' ||\n" +
                            "              coalesce(col_result, '') ||\n" +
                            "              CASE\n" +
                            "                  WHEN col_result IS NOT NULL AND con_result IS NOT NULL THEN E',\\n'\n" +
                            "                  ELSE ''\n" +
                            "                  END ||\n" +
                            "              coalesce(con_result, '') ||\n" +
                            "              E'\\n);';\n" +
                            "\n" +
                            "    RETURN result;\n" +
                            "END;\n" +
                            "$$ LANGUAGE plpgsql;";
                    String queryRights = "GRANT ALL PRIVILEGES ON FUNCTION pg_get_tabledef() TO PUBLIC";
                    query = "SELECT table_name as name, pg_get_tabledef(table_name) as sql FROM information_schema.tables WHERE table_schema='public' AND table_type <> 'VIEW';";
                    statement.execute(queryCreateFunction);
                    statement.execute(queryRights);
                }
                case SQLITE -> query = "SELECT name, sql FROM sqlite_master " +
                        "WHERE type == \"table\" AND name NOT IN ('sqlite_sequence', 'sqlite_stat1', 'sqlite_master')";
                default ->
                        throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
            }
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                tableList.add(new Table(resultSet.getString("name"), resultSet.getString("sql")));
            }
            statement.close();
            return tableList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Index> getIndexes() {
        try {
            List<Index> indexList = new ArrayList<>();
            Statement statement = getStatement();
            String query;
            switch (connectionInfo.getConnectionType()) {
                case POSTGRESQL -> {
                    query = "SELECT table_name as name FROM information_schema.tables WHERE table_schema='public' AND table_type <> 'VIEW'";
                }
                case SQLITE -> {
                    query = "SELECT name FROM sqlite_master " +
                            "WHERE type == \"table\" AND name NOT IN ('sqlite_sequence', 'sqlite_stat1', 'sqlite_master')";
                }
                default ->
                        throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
            }

            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                indexList.addAll(getIndexes(resultSet.getString("name")));
            }
            statement.close();
            return indexList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Index> getIndexes(String tableName) {
        try {
            List<Index> indexList = new ArrayList<>();
            ResultSet resultSet = meta.getIndexInfo(null, null, tableName, false, false);
            Statement statement = getStatement();
            while (resultSet.next()) {
                String name = resultSet.getString("INDEX_NAME");

                if (indexList.stream().anyMatch(x -> x.getName().equals(name))) {
                    continue;
                }

                boolean unique = !resultSet.getBoolean("NON_UNIQUE");

                // Возможно убрать в отдельную функцию получение колонок
                LinkedList<Column> columnLinkedList = new LinkedList<>();
                String queryGetColumnsForIndex;
                switch (connectionInfo.getConnectionType()) {
                    case POSTGRESQL -> {
                        queryGetColumnsForIndex = "SELECT\n" +
                                "    a.attname AS name\n" +
                                "FROM\n" +
                                "    pg_class t,\n" +
                                "    pg_class i,\n" +
                                "    pg_index ix,\n" +
                                "    pg_attribute a,\n" +
                                "    pg_namespace n\n" +
                                "WHERE\n" +
                                "    t.oid = ix.indrelid\n" +
                                "  AND i.oid = ix.indexrelid\n" +
                                "  AND a.attrelid = t.oid\n" +
                                "  AND a.attnum = ANY(ix.indkey)\n" +
                                "  AND t.relnamespace = n.oid\n" +
                                "  AND n.nspname NOT IN ('pg_catalog', 'information_schema')\n" +
                                "  AND i.relname = '" + name + "'\n" +
                                "ORDER BY\n" +
                                "    t.relname,\n" +
                                "    i.relname,\n" +
                                "    a.attnum;";
                    }
                    case SQLITE -> {
                        queryGetColumnsForIndex = "PRAGMA index_info('" + name + "')";
                    }
                    default ->
                            throw new IllegalArgumentException("Unknown connection type: " + connectionInfo.getConnectionType());
                }

                ResultSet columnsNamesResultSet = statement.executeQuery(queryGetColumnsForIndex);
                while (columnsNamesResultSet.next()) {
                    String columnName = columnsNamesResultSet.getString("name");
                    ResultSet columnsResultSet = meta.getColumns(null, null, tableName, columnName);
                    columnsResultSet.next();
                    String dataType = columnsResultSet.getString("TYPE_NAME");
                    boolean notNull = columnsResultSet.getString("IS_NULLABLE").equals("YES");
                    boolean autoInc = columnsResultSet.getString("IS_AUTOINCREMENT").equals("YES");
                    String defaultDefinition = columnsResultSet.getString("COLUMN_DEF");
                    columnLinkedList.addLast(new Column(columnName, dataType, notNull, autoInc, defaultDefinition));
                }

                indexList.add(new Index(name, unique, columnLinkedList));
            }
            statement.close();
            return indexList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Column> getColumns(String tableName) {
        try {
            List<Column> columnList = new ArrayList<>();
            ResultSet resultSet = meta.getColumns(null, null, tableName, null);
            while (resultSet.next()) {
                String name = resultSet.getString("COLUMN_NAME");
                String dataType = resultSet.getString("TYPE_NAME");
                boolean notNull = resultSet.getString("IS_NULLABLE").equals("YES");
                boolean autoInc = resultSet.getString("IS_AUTOINCREMENT").equals("YES");
                String defaultDefinition = resultSet.getString("COLUMN_DEF");
                columnList.add(new Column(name, dataType, notNull, autoInc, defaultDefinition));
            }
            return columnList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ForeignKey> getForeignKeys(String tableName) {
        try {
            List<ForeignKey> foreignKeyList = new ArrayList<>();
            ResultSet rs = meta.getImportedKeys(null, null, tableName);
            int index = 0;
            int previousKeySeq = 1;

            while (rs.next()) {
                String childColumn = rs.getString("FKCOLUMN_NAME");
                String parentColumn = rs.getString("PKCOLUMN_NAME");
                String parentTable = rs.getString("PKTABLE_NAME");
                int currentKeySeq = rs.getInt("KEY_SEQ");

                if (currentKeySeq <= previousKeySeq) {
                    index = index + 1;
                }
                String name = (rs.getString("FK_NAME") == null || rs.getString("FK_NAME").isEmpty()) ?
                        ("FK_" + tableName + "_" + parentTable + "_" + index) : rs.getString("FK_NAME");

                if (currentKeySeq == 1) {
                    previousKeySeq = 1;
                    foreignKeyList.add(new ForeignKey(name, childColumn, parentTable, parentColumn));
                } else {
                    previousKeySeq = currentKeySeq;
                    ForeignKey fk = foreignKeyList.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);

                    if (fk == null) {
                        foreignKeyList.add(new ForeignKey(name, childColumn, parentTable, parentColumn));
                    } else {
                        fk.addChildColumn(childColumn);
                        fk.addParentColumn(parentColumn);
                    }
                }
            }
            return foreignKeyList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Key> getKeys(String tableName) {
        try {
            List<Key> keyList = new ArrayList<>();
            ResultSet resultSet = meta.getPrimaryKeys(null, null, tableName);
            int index = 0;
            int previousKeySeq = 1;

            while (resultSet.next()) {
                String column = resultSet.getString("COLUMN_NAME");
                int currentKeySeq = resultSet.getInt("KEY_SEQ");

                if (currentKeySeq <= previousKeySeq) {
                    index = index + 1;
                }
                String name = (resultSet.getString("PK_NAME") == null ||
                        resultSet.getString("PK_NAME").isEmpty()) ?
                        (tableName + "_PK" + "_" + index) : resultSet.getString("PK_NAME");

                if (currentKeySeq == 1) {
                    previousKeySeq = 1;
                    keyList.add(new Key(name, column));
                } else {
                    previousKeySeq = currentKeySeq;
                    Key key = keyList.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
                    if (key == null) {
                        keyList.add(new Key(name, column));
                    } else {
                        key.addColumn(column);
                    }
                }
            }
            return keyList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DataTable executeQuery(String sql, int rowsToGet) {
        Statement statement = getStatement();
        try {
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            List<String> columnNames = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();
            int rowsGot = 0;

            long startTime = System.currentTimeMillis();
            int columnsNumber = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnsNumber; i++) {
                columnNames.add(resultSetMetaData.getColumnName(i));
            }
            while (rowsGot < rowsToGet && rs.next()) {
                rowsGot++;
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            long executionTime = System.currentTimeMillis() - startTime;
            return new DataTable(columnNames, rows, rs, rowsGot, executionTime);
        } catch (SQLException e) {
            return new DataTable(e.getMessage());
        }
    }

    public void createTable(Table table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + table.getName() + " (");
        boolean f = false;
        for (Column column : table.getColumns()) {
            f = true;
            sql.append(column.getName()).append(" ").append(column.getDataType());
            if (column.isNotNull()) {
                sql.append(" NOT NULL");
            }
            sql.append(",\n");
        }

        for (Index index : table.getIndexes()) {
            f = true;
            sql.append("CONSTRAINT ").append(index.getName()).append(" UNIQUE (");
            for (Column column : index.getColumnLinkedList()) {
                sql.append(column.getName()).append(", ");
            }
            if(index.getColumnLinkedList().size() > 0) {
                sql.setLength(sql.length() - 2);
            }

            sql.append("),\n");
        }

        for (Key key : table.getKeys()) {
            f = true;
            sql.append("CONSTRAINT ").append(key.getName()).append(" PRIMARY KEY (");
            for (String column : key.getColumns()) {
                sql.append(column).append(", ");
            }
            if(!key.getColumns().isEmpty()) {
                sql.setLength(sql.length() - 2);
            }
            sql.append("),\n");
        }

        for (ForeignKey foreignKey : table.getForeignKeys()) {
            f = true;
            sql.append("CONSTRAINT ").append(foreignKey.getName()).append(" FOREIGN KEY (");
            for (String childColumn : foreignKey.getChildColumns()) {
                sql.append(childColumn).append(", ");
            }
            if(!foreignKey.getChildColumns().isEmpty()) {
                sql.setLength(sql.length() - 2);
            }
            sql.append(") REFERENCES ").append(foreignKey.getParentTable()).append(" (");
            for (String parentColumn : foreignKey.getParentColumns()) {
                sql.append(parentColumn).append(", ");
            }
            if(!foreignKey.getParentColumns().isEmpty()) {
                sql.setLength(sql.length() - 2);
            }

            sql.append(") ");
            if (!Objects.equals(foreignKey.getOnDeleteAction(), "")) {
                sql.append("ON DELETE ").append(foreignKey.getOnDeleteAction());
            }
            sql.append(",\n");
        }
        if (f) {
            sql.setLength(sql.length() - 2);
        }


        sql.append(");\n");

        Statement statement = getStatement();
        System.out.println(sql);
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, table.getName(), Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Error creating table: " + e.getMessage());
        }
    }

    public void dropTable(String tableName, boolean ifExists, boolean cascade) {
        StringBuilder sql = new StringBuilder("DROP TABLE ");
        if (ifExists) {
            sql.append("IF EXISTS ");
        }
        sql.append(tableName);
        if (cascade) {
            sql.append(" CASCADE");
        }
        sql.append(";\n");

        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, "", Cancel.CancelType.DELETED, 0));
            statement.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Error drop table", e);
        }
    }

    public void updateTableName(Table table, String newName) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ").append(table.getName())
                .append("\tRENAME TO ").append(newName)
                .append(";\n");
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, table.getName(), Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql.toString());
            table.setName(newName);
        } catch (SQLException e) {
            throw new RuntimeException("Error rename table", e);
        }
    }

    public void updateColumnName(String table, String oldName, String newName) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ").append(table).append("\n");
        sql.append("\tRENAME COLUMN ").append(oldName).append(" TO ").append(newName).append(";\n");
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, table, Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql.toString());

        } catch (SQLException e) {
            throw new RuntimeException("Error rename column", e);
        }
    }

    public void updateColumnComment(String tableName, String columnName, String comment) {
        StringBuilder sql = new StringBuilder("COMMENT ON COLUMN ")
                .append(tableName).append(".").append(columnName)
                .append(" is '").append(comment).append("';\n");
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, tableName, Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Error change comment", e);
        }
    }

    public void updateDataType(String tableName, String columnName, String dataType) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ").append(tableName).append("\n");
        sql.append("\tALTER COLUMN ").append(columnName).append(" type ").append(dataType)
                .append(" USING ").append(columnName).append(":").append(dataType).append(";\n");
        Statement statement = getStatement();
        try {
            Savepoint savepoint = connection.setSavepoint();
            savepoints.add(new Cancel(savepoint, tableName, Cancel.CancelType.TABLE, 0));
            statement.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Error change data type", e);
        }
    }

    @Deprecated
    public DatabaseMetaData getMetaData() {
        return meta;
    }
}