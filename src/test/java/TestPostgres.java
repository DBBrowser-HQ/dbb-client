import app.backend.entities.Connection;
import app.backend.entities.ConnectionInfo;
import app.backend.entities.ConnectionStorage;
import app.backend.entities.DataTable;
import app.backend.entities.Index;
import app.backend.entities.Schema;
import app.backend.entities.Table;
import app.backend.entities.View;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPostgres {
    public static void main(String[] args) throws SQLException, InterruptedException {
        ConnectionStorage conn = new ConnectionStorage();
        Map<String, String> info = new HashMap<>();

        // hosting
//        info.put("host", "db-cloud.ru");
//        info.put("port", "8082");
//        info.put("datasourceId", "1");
//        info.put("accessToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTkxNTQxMjksImlhdCI6MTcxOTE1MDUyOSwidXNlcklkIjoxfQ.v46ZjJ19eDvU4yrB3QAcxmiGnfNkk3pVGt8po1bjbuk");

        // localhost
        info.put("host", "localhost");
        info.put("port", "8082");
        info.put("datasourceId", "2");
        info.put("accessToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTkxNjI2NDQsImlhdCI6MTcxOTE1OTA0NCwidXNlcklkIjoxfQ.2gdCKiJKXtFsyTPJoYLQuwPJB_onl0xBb4egdRPNJ4Q");

        ConnectionInfo connectionInfo = new ConnectionInfo(ConnectionInfo.ConnectionType.POSTGRESQL, info);
        String connectionName = "поставить сюда имя, соответствующее datasource'у, к которому подключаемся";
        Connection connection = new Connection(connectionName, connectionInfo);
        conn.addConnectionToStorage(connection);

        DataTable dataTable = connection.executeQuery("SELECT * FROM example1;");
        System.out.println(dataTable.getMessage());
        System.out.println(dataTable.getColumnNames());
        for (List<String> row : dataTable.getRows()) {
            for (int i = 0; i < dataTable.getColumnNames().size(); i++) {
                System.out.print(row.get(i) + " ");
            }
            System.out.println();
        }

        // GET Tables
        Schema schema = connection.getSchema();
        connection.setTables();
        List<Table> tables = schema.getTables();
        tables.forEach(t -> System.out.println(t.getName() + " " + t.getDefinition()));

//        // Get table data
//        DataTable dataTable = connection.getDataFromTable("example1");
//        System.out.println(dataTable.getMessage());
//        System.out.println(dataTable.getColumnNames());
//        for (List<String> row : dataTable.getRows()) {
//            for (int i = 0; i < dataTable.getColumnNames().size(); i++) {
//                System.out.print(row.get(i) + " ");
//            }
//            System.out.println();
//        }
//
//        // Delete data
//        connection.deleteData("example1", 1);
//        connection.saveChanges();
//
//        // Get updated data
//        dataTable = connection.getDataFromTable("example1");
//        System.out.println(dataTable.getMessage());
//        System.out.println(dataTable.getColumnNames());
//        for (List<String> row : dataTable.getRows()) {
//            for (int i = 0; i < dataTable.getColumnNames().size(); i++) {
//                System.out.print(row.get(i) + " ");
//            }
//            System.out.println();
//        }

        // Get All Indexes
        connection.setIndexes();
        List<Index> indexes = schema.getIndexes();
        indexes.forEach(i -> {
            System.out.println(i.getName() + " " + i.isUnique());
            i.getColumnLinkedList().forEach(c -> System.out.println(c.getName() + " " + c.getDataType()));
        });

        // Get Views
        connection.setViews();
        List<View> views = schema.getViews();
        views.forEach(v -> System.out.println(v.getName() + " " + v.getDefinition()));

        Table table = schema.getTable("example1");
        connection.setForeignKeysFor(table.getName());
        connection.setKeysFor(table.getName());
        connection.setColumnsFor(table.getName());
        connection.setIndexesFor(table.getName());
        table.getColumns().forEach(System.out::println);
        table.getIndexes().forEach(System.out::println);
        table.getForeignKeys().forEach(System.out::println);
        table.getKeys().forEach(System.out::println);

        // Create and delete of Index
        connection.deleteIndex("name_index", "example1");
        connection.createIndex("name_index", "example1", true, List.of("name"));
        connection.saveChanges();

        // Create and delete view
        connection.deleteView("cool_view");
        connection.createView("cool_view", "SELECT * FROM example1");
        connection.createView("cool_view1", "SELECT * FROM example");
        connection.saveChanges();
        // Необязательно, прокси умеет такое обходить ╰(*°▽°*)╯
        // connection.disconnect();
    }
}
