import app.backend.entities.Connection;
import app.backend.entities.ConnectionInfo;
import app.backend.entities.ConnectionStorage;
import app.backend.entities.Schema;
import app.backend.entities.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestTable {
    public static void main(String[] args) {
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTkyMTM0ODQsImlhdCI6MTcxOTIwOTg4NCwidXNlcklkIjoxfQ.OxsYrWX6fZvxRNYNf-fslGma6TfJRQV_o7tSp9gGo1I";
        String datasourceId = "1";
        Connection connection = createConnection("1", datasourceId, accessToken);
        System.out.println(connection.isConnected());

        Schema schema = connection.getSchema();
        connection.setTables();
        System.out.println(schema.getName());
        System.out.println(schema.getTableList());

        connection.newTable("amina", "let's go");

        connection.getSchema().getTable("amina")
                .addColumn("ticket_no", "bpchar(13)", true, "");

        connection.getSchema().getTable("amina")
                .addColumn("flight_id", "int4", true, "");

        connection.getSchema().getTable("amina")
                .addColumn("boarding_no", "int4", true, "");

        connection.getSchema().getTable("amina")
                .addColumn("seat_no", "varchar(4)", true, "");

        connection.getSchema().getTable("amina")
                .addIndex("boarding_passes_flight_id_boarding_no_key2", true, new ArrayList<>(List.of("flight_id", "boarding_no")));

        connection.getSchema().getTable("amina")
                .addIndex("boarding_passes_flight_id_seat_no_key2", true, new ArrayList<>(List.of("flight_id", "seat_no")));

        connection.getSchema().getTable("amina")
                .addKey("boarding_passes_pkey2", new ArrayList<>(List.of("ticket_no", "flight_id")));

//        connection.getSchema().getTable("amina")
//                .addForeignKey("boarding_passes_ticket_no_fkey2", new ArrayList<>(List.of("ticket_no", "flight_id")), "ticket_flights", new ArrayList<>(List.of("ticket_no", "flight_id")), "CASCADE");

        connection.createTable("amina");

        System.out.println(connection.getSchema().getTableList());

        connection.dropTable("amina", false, false);

        System.out.println(connection.getSchema().getTableList());

        connection.saveChanges();
    }

    public static Connection createConnection(String name, String datasourceId, String accessToken) {
        Map<String, String> info = new HashMap<>();
        info.put("host", "db-cloud.ru");
        info.put("port", "8082");
        info.put("datasourceId", datasourceId);
        info.put("accessToken", accessToken);

        ConnectionInfo connectionInfo = new ConnectionInfo(ConnectionInfo.ConnectionType.POSTGRESQL, info);
        return new Connection(name, connectionInfo);
    }
}
