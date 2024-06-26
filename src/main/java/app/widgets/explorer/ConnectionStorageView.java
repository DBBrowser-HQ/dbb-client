package app.widgets.explorer;

import app.IconLoader;
import app.MainWindow;
import app.api.ApiCalls;
import app.api.data.responses.Datasource;
import app.backend.controllers.ConnectionController;
import app.backend.controllers.StorageController;
import app.backend.utility.Saver;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.gui.QAction;
import io.qt.gui.QCursor;
import io.qt.widgets.QMenu;
import io.qt.widgets.QPushButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConnectionStorageView extends QPushButton {

    private final Map<String, ActionForConnectionStorage> connectionList;
    private final Map<String, Integer> datasources;
    private final MainWindow root;
    private final QMenu popMenu;
    private QPoint point;

    public ConnectionStorageView(MainWindow root) {
        this.root = root;
        connectionList = new HashMap<>();
        datasources = new HashMap<>();
        this.setText("Current connection");
        popMenu = new QMenu("Current connection");
        popMenu.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        popMenu.customContextMenuRequested.connect(this::contextMenu);
        this.setMenu(popMenu);
    }

    public void addConnection(String conName) {
        if (!(connectionList.containsKey(conName))) {
            ActionForConnectionStorage action = new ActionForConnectionStorage(conName, root.menuController, this);
            connectionList.put(conName, action);
            popMenu.addAction(connectionList.get(conName));
            this.setText(conName);
        }
    }

    public void addDatasource(Datasource datasource) {
        if (!(connectionList.containsKey(datasource.name))) {
            datasources.put(datasource.name, datasource.id);
            ActionForConnectionStorage action = new ActionForConnectionStorage(datasource.name, root.menuController, this);
            connectionList.put(datasource.name, action);
            popMenu.addAction(connectionList.get(datasource.name));
            this.setText(datasource.name);
        }
    }

    public void clearDS() {
        datasources.clear();
        popMenu.clear();
    }

    public void deleteConnection() {
        var conName = Objects.requireNonNull(popMenu.actionAt(point)).text();
        popMenu.removeAction(connectionList.get(conName));
        if (root.online) {
            this.setText("");
            ApiCalls.deleteDataSource(datasources.get(conName), popMenu::close);
        }
        else {
            connectionList.remove(conName);
            this.setText("");
            if (ConnectionController.isActive(conName)) {
                ConnectionController.closeConnection(conName);
            }
            ConnectionController.deleteCon(conName);
            Saver.saveConnectionStorage(StorageController.connectionStorage);
        }
    }

    public String getCurrentConnection() {
        return this.text();
    }

    void contextMenu(QPoint cPoint) {
        point = cPoint;
        QMenu contextMenu = new QMenu();
        String content;
        if (root.online) {
            content = "Delete datasource";
        }
        else {
            content = "Delete connection";
        }
        QAction delete = new QAction(content);
        delete.triggered.connect(this::deleteConnection);
        contextMenu.addAction(delete);
        contextMenu.popup(QCursor.pos());
    }

    public void setIconActive() {
        try {
            setIcon(IconLoader.loadIconStatic("../active.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setIconDisabled() {
        try {
            setIcon(IconLoader.loadIconStatic("../disabled.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
