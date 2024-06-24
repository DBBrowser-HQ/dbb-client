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
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTkyMzYxNzYsImlhdCI6MTcxOTIzMjU3NiwidXNlcklkIjoxfQ.ZlYn227g4-iIJsFKyhEI9nlHeJf59V4vXxEigS3LS_w";
        String datasourceId = "1";
        Connection connection = createConnection("1", datasourceId, accessToken);
        System.out.println(connection.isConnected());

        Schema schema = connection.getSchema();
        connection.setTables();
        System.out.println(schema.getName());
        System.out.println(schema.getTableList());
        System.out.println(connection.getSchema().getTable("table1"));
        connection.dropTable("amina", true, false);
        connection.dropTable("th", true, false);
        connection.dropTable("table1", true, false);
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

        System.out.println("create" + connection.getSchema().getTableList());

        connection.newTable("th", "ffg");
        connection.createTable("th");
        connection.setTables();
        connection.setColumnsFor("amina");
        System.out.println("create2" + connection.getSchema().getTableList());
        connection.dropTable("amina", false, false);
        connection.setTables();
        System.out.println("drop" + connection.getSchema().getTableList());

        connection.discardChanges();
        connection.setTables();
        connection.setColumnsFor("amina");
        System.out.println("discard" + connection.getSchema().getTableList());

        connection.renameColumn("amina", "seat_no", "jkd");
        connection.setTables();
        connection.setColumnsFor("amina");
        System.out.println(connection.getSchema().getTable("amina").toString());

        connection.renameTable("amina", "table1");
        connection.setTables();
        connection.setColumnsFor("table1");
        System.out.println(connection.getSchema().getTable("table1").toString());

        connection.createIndex("my_index", "table1", true, List.of("jkd"));
        connection.saveChanges();

        connection.setIndexesFor("table1");
        System.out.println(schema.getIndexes());
        connection.disconnect();
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
