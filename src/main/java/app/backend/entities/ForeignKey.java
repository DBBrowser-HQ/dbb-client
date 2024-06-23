package app.backend.entities;

import java.util.ArrayList;
import java.util.List;

public class ForeignKey {
    private String name;
    private List<String> childColumns;
    private String parentTable;
    private List<String> parentColumns;
    private String onDeleteAction;

    public ForeignKey(String name, ArrayList<String> childColumns, String parentTable, ArrayList<String> parentColumns, String onDelete) {
        this.name = name;
        this.childColumns = childColumns;
        this.parentTable = parentTable;
        this.parentColumns = parentColumns;
        this.onDeleteAction = onDelete;
    }

    public void addChildColumn(String columnName) {
        childColumns.add(columnName);
    }

    public void addParentColumn(String columnName) {
        parentColumns.add(columnName);
    }

    public String getName() {
        return name;
    }

    public String getOnDeleteAction(){
        return onDeleteAction;
    }

    public List<String> getChildColumns() {
        return childColumns;
    }

    public String getParentTable() {
        return parentTable;
    }

    public List<String> getParentColumns() {
        return parentColumns;
    }
}
