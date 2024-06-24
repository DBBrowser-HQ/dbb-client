package app.backend.entities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Table {
    private String name;
    private String definition;
    private List<Column> columnList;
    private List<Index> indexList;
    private List<Key> keyList;
    private List<ForeignKey> foreignKeyList;
    private DataTable dataTable;

    public Table(String name, String definition) {
        this.name = name;
        this.definition = definition;
        this.columnList = new ArrayList<>();
        this.indexList = new ArrayList<>();
        this.keyList = new ArrayList<>();
        this.foreignKeyList = new ArrayList<>();
    }

    public DataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setColumnList(List<Column> columnList) {
        this.columnList = columnList;
    }

    public List<String> getColumnList() {
        return columnList.stream().map(Column::getName).toList();
    }

    public Column getColumn(String columnName) {
        return columnList.stream().filter(x -> x.getName().equals(columnName)).findFirst().orElse(null);
    }

    public List<Column> getColumns() {
        return columnList;
    }

    public void setIndexList(List<Index> indexList) {
        this.indexList = indexList;
    }

    public List<String> getIndexList() {
        return indexList.stream().map(Index::getName).toList();
    }

    public Index getIndex(String indexName) {
        return indexList.stream().filter(x -> x.getName().equals(indexName)).findFirst().orElse(null);
    }

    public List<Index> getIndexes() {
        return indexList;
    }

    public void setKeyList(List<Key> keyList) {
        this.keyList = keyList;
    }

    public List<String> getKeyList() {
        return keyList.stream().map(Key::getName).toList();
    }

    public Key getKey(String keyName) {
        return keyList.stream().filter(x -> x.getName().equals(keyName)).findFirst().orElse(null);
    }

    public List<Key> getKeys() {
        return keyList;
    }

    public void setForeignKeyList(List<ForeignKey> foreignKeyList) {
        this.foreignKeyList = foreignKeyList;
    }

    public List<String> getForeignKeyList() {
        return foreignKeyList.stream().map(ForeignKey::getName).toList();
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeyList;
    }

    public void addColumn(String columnName, String dataType, boolean notNull, String defaultDefinition) {
        Column column = new Column(columnName, dataType, notNull, false, defaultDefinition);
        this.columnList.add(column);
    }

    public void addKey(String name, ArrayList<String> columns) {
        Key key = new Key(name, columns);
        this.keyList.add(key);

    }

    public void addForeignKey(String name, ArrayList<String> childColumns, String parentTable, ArrayList<String> parentColumns, String onDeleteAction) {
        this.foreignKeyList.add(new ForeignKey(name, childColumns, parentTable, parentColumns, onDeleteAction));

    }

    public void addIndex(String name, boolean unique, ArrayList<String> columnList) {
        LinkedList<Column> columnLinkedList = new LinkedList<>();
        for (String col : columnList) {
            for (Column column : this.columnList) {
                if (column.getName().equals(col)) {
                    columnLinkedList.add(column);
                }
            }
        }
        if (columnList.size() != columnLinkedList.size()) {
            throw new RuntimeException("Different column list sizes: columnList has nonexistent column name");
        }
        this.indexList.add(new Index(name, unique, columnLinkedList));
    }
}