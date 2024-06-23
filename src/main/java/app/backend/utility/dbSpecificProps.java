package app.backend.utility;

import java.util.List;

public class dbSpecificProps {
    public static List<String> getSQLiteProps() {
        return List.of("url");
    }

    public static List<String> getPostgresSQLProps() {
        return List.of("accessToken", "host", "port", "datasourceId");
    }
}
