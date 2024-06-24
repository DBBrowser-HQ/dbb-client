package app.backend.entities;

import java.io.Serial;
import java.io.Serializable;
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
//        var t = new Thread(this::connect);
//        t.start();
//        try {
//            sleep(1000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void connect() {
        if (session != null) {
            session.disconnect();
        }
        this.session = new Session(connectionInfo);
        this.isConnected = session.isConnected();
        this.supportsSchema = session.isSupportsSchema();
//        if (supportsSchema) {
//            setDatabaseList();
//        } else {
//            setSchema();
//        }
        setSchema();
    }

    public void disconnect() {
        session.disconnect();
        session = null;
        isConnected = false;
    }

    public void reconnect() {
        session.reconnect(connectionInfo);
        this.isConnected = session.isConnected();
        this.supportsSchema = session.isSupportsSchema();
//        if (supportsSchema) {
//            setDatabaseList();
//        } else {
//            setSchema();
//        }
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
//        if (supportsSchema) {
//            setDatabaseList();
//        } else {
//            setSchema();
//        }
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

    public DataTable insertData(String tableName, List<String> newValues) {
        session.insertData(tableName, newValues, schema.getTable(tableName).getDataTable().getColumnNames());
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
        schema.setIndexList(schema.getIndexes().stream().filter(index -> {
            if (index.getStatusDDL() == 1) {
                return false;
            } else if (index.getStatusDDL() == -1) {
                index.setStatusDDL(0);
            }
            return true;
        }).toList());

        this.getSchema().getTables().forEach(table -> {
            if (table.getIndexes() != null) {
                table.setIndexList(table.getIndexes().stream().filter(index -> {
                    if (index.getStatusDDL() == 1) {
                        return false;
                    } else if (index.getStatusDDL() == -1) {
                        index.setStatusDDL(0);
                    }
                    return true;
                }).toList());
            }
        });
    }

    // Для подтверждения сохранения и удаления
    private void commitIndexes() {
        schema.setIndexList(schema.getIndexes().stream().filter(index -> {
            if (index.getStatusDDL() == -1) {
                return false;
            } else if (index.getStatusDDL() == 1) {
                index.setStatusDDL(0);
            }
            return true;
        }).toList());

        this.getSchema().getTables().forEach(table -> {
            if (table.getIndexes() != null) {
                table.setIndexList(table.getIndexes().stream().filter(index -> {
                    if (index.getStatusDDL() == -1) {
                        return false;
                    } else if (index.getStatusDDL() == 1) {
                        index.setStatusDDL(0);
                    }
                    return true;
                }).toList());
            }
        });
    }

    public void createView(String viewName, String sql) {
        session.createView(viewName, sql);
        View view = new View(viewName, sql);
        view.setStatusDDL(1);
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
        schema.setViewList(schema.getViews().stream().filter(view -> {
            if (view.getStatusDDL() == 1) {
                return false;
            } else if (view.getStatusDDL() == -1) {
                view.setStatusDDL(0);
            }
            return true;
        }).toList());
    }

    private void commitViews() {
        schema.setViewList(schema.getViews().stream().filter(view -> {
            if (view.getStatusDDL() == -1) {
                return false;
            } else if (view.getStatusDDL() == 1) {
                view.setStatusDDL(0);
            }
            return true;
        }).toList());
    }

    public void saveChanges() {
        commitIndexes();
        commitViews();
        session.saveChanges();
    }

    public void discardChanges() {
        rollbackIndexes();
        rollbackViews();
        session.discardChanges();
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
}
