package app.backend;



import app.backend.entities.Connection;
import app.backend.entities.ConnectionInfo;
import app.backend.entities.ConnectionStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestTable {
    public static void main(String[] args) {

        ConnectionStorage connectionStorage = new ConnectionStorage();
        Map<String, String> info = new HashMap<>();
        info.put("username", "postgres");
        info.put("password", "652385");
        info.put("host", "localhost");
        info.put("port", "5432");
        info.put("dbname", "demo");
        ConnectionInfo connectionInfo = new ConnectionInfo(ConnectionInfo.ConnectionType.POSTGRESQL, info);
        connectionStorage.addConnectionToStorage(new Connection("postgres", connectionInfo));
        Connection connection = connectionStorage.getConnection("postgres");
        System.out.println(connection.isConnected());

        connection.setDatabaseList();
        connection.setSchema("bookings");
        connection.setTables();
        System.out.println(connection.getDatabaseList());
        System.out.println(connection.getSchema().getName());
        System.out.println(connection.getSchema().getTableList());


        connection.newTable("amina", "let's go");
        connection.addColumn("amina", "ticket_no", "bpchar(13)", true, "");
        connection.addColumn("amina", "flight_id", "int4", true, "");
        connection.addColumn("amina", "boarding_no", "int4", true, "");
        connection.addColumn("amina", "seat_no", "varchar(4)", true, "");
        connection.addIndex("amina", "boarding_passes_flight_id_boarding_no_key2", true, new ArrayList<>(List.of("flight_id", "boarding_no")));
        connection.addIndex("amina", "boarding_passes_flight_id_seat_no_key2", true, new ArrayList<>(List.of("flight_id", "seat_no")));

        connection.addKey("amina", "boarding_passes_pkey2", new ArrayList<>(List.of("ticket_no", "flight_id")));

        connection.addForeignKey("amina", "boarding_passes_ticket_no_fkey2", new ArrayList<>(List.of("ticket_no", "flight_id")), "ticket_flights", new ArrayList<>(List.of("ticket_no", "flight_id")), "CASCADE");

        connection.createTable("amina");

        System.out.println(connection.getSchema().getTableList());

        connection.dropTable("amina", false, false);

        System.out.println(connection.getSchema().getTableList());

        connection.saveChanges();
    }
}
