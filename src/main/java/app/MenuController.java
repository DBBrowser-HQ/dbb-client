package app;

import app.api.ApiCalls;
import app.api.UserDataRepository;
import app.api.data.responses.Datasource;
import app.backend.controllers.ConnectionController;
import app.backend.controllers.StorageController;
import app.backend.entities.ConnectionInfo;
import app.backend.entities.DataTable;
import app.backend.utility.Saver;
import app.widgets.dialogs.*;
import app.widgets.dialogs.settings.SettingsDialog;
import io.qt.core.QObject;
import io.qt.gui.QCursor;
import io.qt.widgets.QCheckBox;
import io.qt.widgets.QMessageBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static java.lang.Thread.sleep;

public class MenuController extends QObject {

    public final MainWindow root;
    private final Signal1<List<Datasource>> signalOk = new Signal1<>();
    private final Signal1<String> signalErr = new Signal1<>();

    public MenuController(MainWindow mainWindow) {
        root = mainWindow;
        signalOk.connect(this, "datasourcesLoaded(List)");
        signalErr.connect(this, "error(String)");
    }

    public void loadDatasources() {
        ApiCalls.getDatasources(signalOk, signalErr);
    }

    void run_clicked() throws SQLException {
        if (root.edit.toPlainText().isEmpty()) {
            root.label.setText(" Empty text box. Write something.");
        }
        else {
            if (root.dbName.toPlainText().isEmpty() || (!(ConnectionController.isActive(root.dbName.toPlainText())))) {
                root.label.setText("No connection to database");
            }
            else {

                DataTable resultTable =
                    ConnectionController.execQuery(root.dbName.toPlainText(), root.edit.toPlainText());
                if (resultTable.getRows() == null) {
                    root.label.setText(resultTable.getMessage());
                }
                else {
                    root.tableView.setTable(resultTable);
                    root.label.setText(" Your query done. " + resultTable.getMessage());
                }
            }
        }
    }

    void act1() {
        root.label.setText(" DON'T TOUCH ME!!!");
    }

    void callTextBox() {
        QMessageBox.StandardButton reply =
            QMessageBox.question(root, "Tile", "Cringe text box",
                new QMessageBox.StandardButtons(QMessageBox.StandardButton.Yes, QMessageBox.StandardButton.No));
        if (reply == QMessageBox.StandardButton.Yes) {
            System.out.println("User said Yes");
        }
        else {
            System.out.println("User said NO");
        }
    }

    void callCustomCheckBox() {
        new CustomCheckBoxDialog(root);
        root.setEnabled(false);
    }

    //Вот эта штука на фоне моей кастомной вообще юзлес, наверное уберём её.
    void callDefaultCheckBox() {
        QMessageBox messageBox = new QMessageBox();
        messageBox.setText("Cringe CheckBox");
        messageBox.setWindowTitle("Title");
        QCheckBox checkBox1 = new QCheckBox();
        checkBox1.setText("Point1");
        messageBox.setCheckBox(checkBox1);
        messageBox.setStandardButtons(QMessageBox.StandardButton.Apply, QMessageBox.StandardButton.Abort);
        CheckBoxChecker check1 = new CheckBoxChecker();
        checkBox1.stateChanged.connect(check1, "inverse()");

        int reply = messageBox.exec();

        if (QMessageBox.StandardButton.resolve(reply) == QMessageBox.StandardButton.Apply) {
            if (check1.getValue()) {
                System.out.println("User pick it");
            } else {
                System.out.println("User DIDN'T");
            }
        }
    }

    void clear_button_clicked() {
        root.label.clear();
        root.edit.clear();
    }

    void rightClick() {
        root.popMenu.popup(QCursor.pos());
    }

    void connectToDBButtonClicked() {

        new FileDialog(this);

    }

    void fileChosen(String fileName) {
        if (root.online) {
            StorageController.connectionStorage.getConnection(root.connectionStorageView.getCurrentConnection()).connect();
        }
        else {
            if (fileName.endsWith(".db")) {
                StorageController.connectionStorage.removeConnection(fileName);
                ConnectionController.addConnection(ConnectionInfo.ConnectionType.SQLITE, fileName);
            }
            else {
                new ErrorDialog("Chosen file is not a database file.");
            }
        }
    }

    public void showSchema(String conName) throws IOException {
        var x = ConnectionController.getSchema(conName);
        if (ConnectionController.isActive(conName)) {
            root.connectionStorageView.setIconActive();
        }
        else {
            root.connectionStorageView.setIconDisabled();
        }
        root.treeViewMenu.setTreeModel(x, conName);
    }

    public void addTable() {
        new CreateTableDialog(this, root.windowIcon());
    }

    public void clearWorkArea() {
        root.connectionStorageView.setIconDisabled();
        root.treeViewMenu.setEmptyModel();
        root.dbName.clear();
        root.dbInfo.clear();
        root.tableViewMainTab.clear();
        root.currentTableName.setText("");
        root.tableMessage.clear();
    }

    void closeConnectionButtonClicked() {
        ConnectionController.closeConnection(root.connectionStorageView.getCurrentConnection());
        clearWorkArea();
    }

    void reconnectToDBClicked() {
        StorageController.connectionStorage.getConnection(root.connectionStorageView.getCurrentConnection()).connect();
        try {
            newCurrentConnectionName(root.connectionStorageView.getCurrentConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showDBInfo() {

        String list = ConnectionController.getDBInfo(root.dbName.toPlainText());
        root.dbInfo.setText(list);
    }

    public void newConnectionName(String name) {
        root.dbName.setText(name);
        root.tableViewMainTab.clear();
        root.connectionStorageView.addConnection(name);
        Saver.saveConnectionStorage(StorageController.connectionStorage);
    }

    public void setTableDataView(DataTable table, String tableName) {
        root.tableViewMainTab.setTableView(table, tableName);
        root.showTableViewButtons();
        root.currentTableName.setText(tableName);
        root.tableMessage.setText(table.getMessage());

        root.infoTab.setOutput(ConnectionController.getDDL(root.dbName.toPlainText(), tableName),
            ConnectionController.getKeys(root.dbName.toPlainText(), tableName).toString(),
            ConnectionController.getForeignKeys(root.dbName.toPlainText(), tableName).toString()
        );
    }

    private void setInfoTab() {

    }

   public void newCurrentConnectionName(String conName) throws IOException {
        root.dbName.setText(conName);
        root.dbInfo.setText("PostreSQL");
        root.treeViewMenu.newCurrentConnectionName(conName);
        root.tableViewMainTab.clear();
        root.currentTableName.setText("");
        showSchema(conName);
   }

   public void deleteConnection(String conName) {

   }

   void moreRows(Integer id) {
        if (id == 1) {
            root.tableViewMainTab.moreRows();
            root.tableMessage.setText(root.tableViewMainTab.getMessage());
        }
        else {
            root.tableView.moreRows();
            root.label.setText(root.tableView.getMessage());
        }

   }

   void saveChanges() {
        ConnectionController.saveChanges(root.dbName.toPlainText());
   }

   void discardChanges() {
        if (root.tableViewMainTab.getCurrentTableName() == null) {
            ConnectionController.discardChanges(root.dbName.toPlainText());
        }
        else {
            ConnectionController.discardChanges(root.dbName.toPlainText(), root.tableViewMainTab.getCurrentTableName());
        }
   }

   void changeData(String tableName, Integer row, List<Integer> columns, List<String> data) {
        ConnectionController.changeData(root.dbName.toPlainText(), tableName, row, columns, data);
   }

   void deleteRow(String tableName, Integer row) {
       ConnectionController.deleteRow(this.root.connectionStorageView.getCurrentConnection(), tableName, row);
   }

   void addRowButtonClicked() {
       new AddRowDialog(root);
   }

   void changeLoadRowsNumberClicked() {
        new ChangeLoadRowsDialog(this);
   }

    public void changeLoadRowsNumber(int number) {
        root.tableViewMainTab.changeRowsToLoadNumber(number);
        root.tableView.changeRowsToLoadNumber(number);
    }

    void settingsClicked() {
        new SettingsDialog(root.windowIcon(), () -> {loadDatasources(); updateRoot(true); root.setWindowTitle(UserDataRepository.currentCompany.second);}, this);
    }

    public void updateRoot(boolean full) {
        root.update(full);
    }

    void datasourcesLoaded(List<Datasource> datasources) {
        if (datasources.isEmpty()) {
            root.treeViewMenu.setEmptyModelOnline();
            root.connectionStorageView.setText(" ");
            root.connectionStorageView.clearDS();
        }
        else {
            root.treeViewMenu.setEmptyModel();
            root.connectionStorageView.clearDS();
            for (Datasource ds : datasources) {
                root.connectionStorageView.addDatasource(ds);
                if (StorageController.connectionStorage.getConnection(ds.name) == null) {
                    ConnectionController.addConnection(ConnectionInfo.ConnectionType.POSTGRESQL, ds.name, Config.host, Config.proxyPort, UserDataRepository.accessToken, String.valueOf(ds.id));
                }
            }
        }
        var conName = root.connectionStorageView.getCurrentConnection();
        try {
            if (ConnectionController.isActive(conName)) {
                try {
                    //root.treeViewMenu.setTreeModel(ConnectionController.getSchema(conName), conName);
                    this.newCurrentConnectionName(conName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (NullPointerException e) {
            root.connectionStorageView.setIconDisabled();
        }

    }

    public void close() {
        ApiCalls.logout();
        root.close();
    }

    void error(String err) {
        new ErrorDialog(err);
    }

}
