package app.backend.entities;

public class Column {
    private String name;
    private String dataType;
    private boolean notNull;
    private String defaultDefinition;

    public Column(String name, String dataType, boolean notNull, String defaultDefinition) {
        this.name = name;
        this.dataType = dataType;
        this.notNull = notNull;
        this.defaultDefinition = defaultDefinition;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public String getDefaultDefinition() {
        return defaultDefinition;
    }
}
