package app.backend.entities;

import java.sql.Savepoint;

public class Cancel {
    private final Savepoint savepoint;
    private final String name;
    private final CancelType type;
    private final Integer index;

    public enum CancelType {
        TABLE,
        INDEX,
        VIEW,
        DELETED
    }

    public Cancel(Savepoint savepoint, String name, CancelType type, Integer index) {
        this.savepoint = savepoint;
        this.name = name;
        this.type = type;
        this.index = index;
    }

    public Savepoint getSavepoint() {
        return savepoint;
    }

    public CancelType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Integer getIndex() {
        return index;
    }
}
