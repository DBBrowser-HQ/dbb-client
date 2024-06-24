package app.backend.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

public class Connection implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static int DEFAULT_ROWS_TO_GET = 100;
    private String name;
    private transient boolean isConnected;
    private ConnectionInfo connectionInfo;
    private transient List<Database> databaseList;
    private transient Schema schema;
    private transient Session session;

    // в зависимости от этой переменной будем выводить сразу содержимое схемы или список баз данных
    // UPD: решил, что в зависимости от этой переменной будем определять PostgresSQL или нет
    // UPD2: сейчас не нужно, можно просто по connectionType
    private boolean supportsSchema;

    public Connection(String name, ConnectionInfo connectionInfo) {
        this.name = name;
        this.connectionInfo = connectionInfo;
        connect();
    }

    public void connect() {
        if (session != null) {
            session.disconnect();
        }
        this.session = new Session(connectionInfo);
        this.isConnected = session.isConnected();
        this.supportsSchema = session.isSupportsSchema();
        setSchema();
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect();
        }
        session = null;
        isConnected = false;
    }

    public void reconnect() {
        session.reconnect(connectionInfo);
        this.isConnected = session.isConnected();
        this.supportsSchema = session.isSupportsSchema();
        setSchema();
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.session.reconnect(connectionInfo);
        this.isConnected = session.isConnected();
        this.supportsSchema = session.isSupportsSchema();
        setSchema();
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public List<String> getDatabaseList() {
        return databaseList.stream().map(Database::getName).toList();
    }

    @Deprecated
    public Database getDatabase(String name) {
        return databaseList.stream()
                .filter(element -> element.getName().equals(name))
                .findFirst().orElse(null);
    }

    @Deprecated
    public void setDatabaseList() {
        this.databaseList = session.getDatabases();
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema() {
        String name = "default";
        switch (connectionInfo.getConnectionType()) {
            case SQLITE -> name = "schema";
            case POSTGRESQL -> name = "public";
        }
        this.schema = new Schema(name);
    }

    public DataTable getDataFromTable(String tableName) {
        DataTable dataTable = session.getDataFromTable(tableName, DEFAULT_ROWS_TO_GET);
        schema.getTable(tableName).setDataTable(dataTable);
        return dataTable;
    }

    public DataTable getDataFromView(String viewName) {
        DataTable dataTable = session.getDataFromTable(viewName, DEFAULT_ROWS_TO_GET);
        schema.getView(viewName).setDataTable(dataTable);
        return dataTable;
    }

    public DataTable executeQuery(String sql) {
        return session.executeQuery(sql, DEFAULT_ROWS_TO_GET);
    }

    /* TODO: Передавать сюда уже заполненный объект таблицы
        или нет, решай сама
     */
    public void newTable(String tableName, String definition) {
        if(schema.getTable(tableName) == null){
            schema.getTables().add(new Table(tableName, definition));
        }
        else{
            throw new RuntimeException("Table " + tableName + " already exists");
        }
    }

    public void createTable(String tableName) {
        for (Table table : schema.getTables()) {
            if (table.getName().equals(tableName)) {
                session.createTable(table);
                return;
            }
        }
    }

    public void dropTable(String tableName, boolean isExist, boolean cascade) {
        session.dropTable(tableName, isExist, cascade);
        schema.getTables().removeIf(table -> table.getName().equals(tableName));
    }

    public DataTable insertData(String tableName, List<String> newValues) {
        Table table = schema.getTable(tableName);
        session.insertData(tableName, newValues, table.getDataTable().getColumnNames());
        return getDataFromTable(tableName);
    }

    public DataTable deleteData(String tableName, int index) {
        DataTable dataTable = schema.getTable(tableName).getDataTable();
        session.deleteData(tableName, dataTable, index);
        return getDataFromTable(tableName);
    }

    public DataTable updateData(String tableName, int rowNumber, List<Integer> columnNumbers, List<String> values) {
        DataTable dataTable = schema.getTable(tableName).getDataTable();
        session.updateData(tableName, dataTable, rowNumber, columnNumbers, values);
        return getDataFromTable(tableName);
    }

    public void createIndex(String indexName, String tableName, boolean isUnique, List<String> columnsNames) {
        session.createIndex(indexName, tableName, isUnique, columnsNames);
        Table table = schema.getTable(tableName);
        this.setColumnsFor(tableName);
        List<Column> columns = table.getColumns().stream().filter(c -> columnsNames.contains(c.getName())).toList();
        LinkedList<Column> columnLinkedList = new LinkedList<>();
        for (String name : columnsNames) {
            columnLinkedList.addLast(columns.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null));
        }
        Index newIndex = new Index(indexName, isUnique, columnLinkedList);
        newIndex.setStatusDDL(1);

        table.getIndexes().add(newIndex);
        schema.getIndexes().add(newIndex);
    }

    public void deleteIndex(String indexName, String tableName) {
        if (schema.getTable(tableName).getIndex(indexName) == null || schema.getIndex(indexName) == null) {
            return;
        }
        session.deleteIndex(indexName);
        schema.getTable(tableName).getIndex(indexName).setStatusDDL(-1);
        schema.getIndex(indexName).setStatusDDL(-1);
    }

    // Для отката создания и удаления
    private void rollbackIndexes() {
        List<Index> list = schema.getIndexes().stream().filter(index -> {
            if (index.getStatusDDL() == 1) {
                return false;
            } else if (index.getStatusDDL() == -1) {
                index.setStatusDDL(0);
            }
            return true;
        }).toList();
        schema.setIndexList(new ArrayList<>(list));

        this.getSchema().getTables().forEach(table -> {
            if (table.getIndexes() != null) {
                List<Index> indexesOfTable = table.getIndexes().stream().filter(index -> {
                    if (index.getStatusDDL() == 1) {
                        return false;
                    } else if (index.getStatusDDL() == -1) {
                        index.setStatusDDL(0);
                    }
                    return true;
                }).toList();
                table.setIndexList(new ArrayList<>(indexesOfTable));
            }
        });
    }

    // Для подтверждения сохранения и удаления
    private void commitIndexes() {
        List<Index> list = schema.getIndexes().stream().filter(index -> {
            if (index.getStatusDDL() == -1) {
                return false;
            } else if (index.getStatusDDL() == 1) {
                index.setStatusDDL(0);
            }
            return true;
        }).toList();

        schema.setIndexList(new ArrayList<>(list));

        this.getSchema().getTables().forEach(table -> {
            if (table.getIndexes() != null) {
                List<Index> indexesInTable = table.getIndexes().stream().filter(index -> {
                    if (index.getStatusDDL() == -1) {
                        return false;
                    } else if (index.getStatusDDL() == 1) {
                        index.setStatusDDL(0);
                    }
                    return true;
                }).toList();
                table.setIndexList(new ArrayList<>(indexesInTable));
            }
        });
    }

    public void createView(String viewName, String sql) {
        session.createView(viewName, sql);
        View view = new View(viewName, sql);
        view.setStatusDDL(1);
        System.out.println(schema.getViews().getClass().getName());
        schema.getViews().add(view);
    }

    public void deleteView(String viewName) {
        if (schema.getView(viewName) == null) {
            return;
        }
        session.deleteView(viewName);
        schema.getView(viewName).setStatusDDL(-1);
    }

    private void rollbackViews() {
        List<View> views = schema.getViews().stream().filter(view -> {
            if (view.getStatusDDL() == 1) {
                return false;
            } else if (view.getStatusDDL() == -1) {
                view.setStatusDDL(0);
            }
            return true;
        }).toList();
        schema.setViewList(new ArrayList<>(views));
    }

    private void commitViews() {
        List<View> views = schema.getViews().stream().filter(view -> {
            if (view.getStatusDDL() == -1) {
                return false;
            } else if (view.getStatusDDL() == 1) {
                view.setStatusDDL(0);
            }
            return true;
        }).toList();
        schema.setViewList(new ArrayList<>(views));
    }

    public void saveChanges() {
//        commitIndexes();
//        commitViews();
        session.saveChanges();
    }

    public ArrayList<String> discardChanges() {
//        rollbackIndexes();
//        rollbackViews();
        Cancel cancel = session.discardChanges();
        if (cancel == null){
            return null;
        }

        return new ArrayList<>(List.of(cancel.getName(), cancel.getIndex().toString()));
    }

    @Deprecated
    public void setSchemasFor(String databaseName) {
        List<Schema> schemaList = session.getSchemas(databaseName);
        getDatabase(databaseName).setSchemaList(schemaList);
    }

    // SQLITE SPECIFIC FUNCTIONS
    public void setViews() {
        List<View> viewList = session.getViews();
        schema.setViewList(viewList);
    }

    public void setTables() {
        try {
            List<Table> tableList = session.getTables();
            schema.setTableList(tableList);
        } catch (NullPointerException e) {
            try {
                sleep(50);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            setTables();
        }
    }

    public void setIndexes() {
        try {
            List<Index> indexList = session.getIndexes();
            schema.setIndexList(indexList);
        } catch (NullPointerException e) {
            try {
                sleep(50);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            setIndexes();
        }
    }

    public void setIndexesFor(String tableName) {
        List<Index> indexList = session.getIndexes(tableName);
        Table table = schema.getTable(tableName);
        if (table == null) {
            throw new RuntimeException("No such table in schema. Possible solution: use setTables()");
        }
        table.setIndexList(indexList);
    }

    public void setColumnsFor(String tableName) {
        List<Column> columnList = session.getColumns(tableName);
        Table table = schema.getTable(tableName);
        if (table == null) {
            throw new RuntimeException("No such table in schema. Possible solution: use setTables()");
        }
        table.setColumnList(columnList);
    }

    public void setForeignKeysFor(String tableName) {
        List<ForeignKey> foreignKeyList = session.getForeignKeys(tableName);
        Table table = schema.getTable(tableName);
        if (table == null) {
            throw new RuntimeException("No such table in schema. Possible solution: use setTables()");
        }
        table.setForeignKeyList(foreignKeyList);
    }

    public void setKeysFor(String tableName) {
        List<Key> keyList = session.getKeys(tableName);
        Table table = schema.getTable(tableName);
        if (table == null) {
            throw new RuntimeException("No such table in schema. Possible solution: use setTables()");
        }
        table.setKeyList(keyList);
    }

    public boolean isSupportsSchema() {
        return supportsSchema;
    }

    @Deprecated
    public Session getSession() {
        return session;
    }


    @Override
    public String toString() {
        return "Connection{" +
                "name='" + name + '\'' +
                ", isConnected=" + isConnected +
                ", connectionInfo=" + connectionInfo +
                ", databaseList=" + databaseList +
                ", schema=" + schema +
                ", session=" + session +
                ", supportsDatabaseAndSchema=" + supportsSchema +
                '}';
    }

    public void renameTable(String tableName, String newName){
        session.updateTableName(schema.getTable(tableName), newName);
    }

    public void renameColumn(String tableName, String oldName, String newName) {
        session.updateColumnName(tableName, oldName, newName);
    }

    public void updateColumnComment(String tableName, String columnName, String comment) {
        session.updateColumnComment(tableName, columnName, comment);
    }

    public void updateColumnType(String tableName, String columnName, String type) {
        session.updateDataType(tableName, columnName, type);
    }
}
