import app.backend.entities.Connection;
import app.backend.entities.ConnectionInfo;
import app.backend.entities.DataTable;
import app.backend.entities.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConcurrentTest {
    public static void main(String[] args) {
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MTkxODA0OTMsImlhdCI6MTcxOTE3Njg5MywidXNlcklkIjoyfQ.caiXnLguEZzxhSXdD0m5O26zA_LB3CGgcmfiVD11YlM";
        String datasourceId1 = "3";
        String datasourceId2 = "4";

        Connection connection1 = createConnection("1", datasourceId1, accessToken);
        Connection connection2 = createConnection("2", datasourceId2, accessToken);

        DataTable dataTable1 = connection1.executeQuery("SELECT current_database()");
        showDataTable(dataTable1);

        DataTable dataTable2 = connection2.executeQuery("SELECT current_database()");
        showDataTable(dataTable2);
        connection1.disconnect();
        connection2.disconnect();
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

    public static void showDataTable(DataTable dataTable) {
        System.out.println(dataTable.getMessage());
        System.out.println(dataTable.getColumnNames());
        for (List<String> row : dataTable.getRows()) {
            for (String val : row) {
                System.out.print(val + " ");
            }
            System.out.println();
        }
    }
}
