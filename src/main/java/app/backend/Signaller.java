package app.backend;

import app.MenuController;
import app.backend.entities.DataTable;
import io.qt.core.QObject;

public class Signaller extends QObject {

    private final Signal1<String> signalToShowTree = new Signal1<>();
    private final Signal0 signalToHideTree = new Signal0();
    private final Signal0 signalToShowDBInfo = new Signal0();
    private final Signal1<String> signalNewConnectionName = new Signal1<>();
    private final Signal2<DataTable, String> signalGetTableData = new Signal2<>();
    private final Signal1<String> signalDeleteConnection = new Signal1<>();

    public Signaller(MenuController controller) {
        signalToShowTree.connect(controller, "showSchema(String)");
        signalToHideTree.connect(controller, "clearWorkArea()");
        signalToShowDBInfo.connect(controller, "showDBInfo()");
        signalNewConnectionName.connect(controller, "newConnectionName(String)");
        signalGetTableData.connect(controller, "setTableDataView(DataTable, String)");
        signalDeleteConnection.connect(controller, "deleteConnection(String)");
    }

    public void emitSignalToShowTree(String dbName) {
        signalToShowTree.emit(dbName);
    }

    public void emitSignalToHideTree() {
        signalToHideTree.emit();
    }

    public void emitSignalToAddConnectionName(String name) {
        signalNewConnectionName.emit(name);
    }

    public void emitSignalToDBInfo() {
        signalToShowDBInfo.emit();
    }

    public void emitSignalToGetTableData(DataTable table, String tableName) {
        signalGetTableData.emit(table, tableName);
    }

    public void emitSignalToDeleteConnection(String conName) {
        signalDeleteConnection.emit(conName);
    }

}

