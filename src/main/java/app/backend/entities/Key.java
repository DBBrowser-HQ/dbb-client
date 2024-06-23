package app.backend.entities;

import java.util.ArrayList;
import java.util.List;

public class Key {
    private String name;
    private List<String> columns;

    public Key(String name, String columns) {
        this.name = name;
        this.columns = new ArrayList<>(List.of(columns));
    }

    public Key(String name, ArrayList<String> columns) {
        this.name = name;
        this.columns = columns;
    }

    public void addColumn(String column) {
        columns.add(column);
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }
}
