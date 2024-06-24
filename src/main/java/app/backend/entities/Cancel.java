package app.backend.entities;

import java.sql.Savepoint;

public class Cancel {
    private final Savepoint savepoint;
    private final String tableName;
    private final Integer index;

    public Cancel(Savepoint savepoint, String tableName, Integer index) {
        this.savepoint = savepoint;
        this.tableName = tableName;
        this.index = index;
    }

    public Savepoint getSavepoint() {
        return savepoint;
    }

    public String getTableName() {
        return tableName;
    }

    public Integer getIndex() {
        return index;
    }
}
