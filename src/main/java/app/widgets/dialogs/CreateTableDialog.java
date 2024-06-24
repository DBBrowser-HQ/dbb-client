package app.widgets.dialogs;

import app.MenuController;
import app.backend.controllers.ConnectionController;
import app.backend.controllers.StorageController;
import app.widgets.MyToolBar;

import app.widgets.dialogs.settings.Setting;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.*;

import java.util.*;


public class CreateTableDialog extends Setting {

    private final MenuController controller;
    private final QTextEdit tableName;
    private final MyToolBar mainBar;
    private final QTableWidget columns;
    private final QScrollArea area;
    private final List<String> tables;
    private final Map<String, List<String>> fColumns;


    public CreateTableDialog(MenuController controller, QIcon icon) {

        this.controller = controller;
        tables = ConnectionController.getSchema(controller.root.connectionStorageView.getCurrentConnection());
        fColumns = new HashMap<>();
        for (var t : tables) {
            fColumns.put(t, ConnectionController.getColumns(controller.root.connectionStorageView.getCurrentConnection(), t));
        }
        this.setMinimumSize(900, 400);
        setWindowTitle("Create new table");
        setWindowIcon(icon);
        mainBar = new MyToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        tableName = new QTextEdit("Table name");
        tableName.setMaximumHeight(27);
        mainBar.addWidget(tableName);
        mainBar.addWidget(new QLabel("Columns:"));
        columns = new QTableWidget(1, 8);
        columns.setHorizontalHeaderLabels(titles());
        columns.setSizePolicy(expandingSizePolicy());
        addColumnLine(true);

        area = new QScrollArea();
        area.setWidget(columns);
        area.setWidgetResizable(true);
        area.setSizePolicy(expandingSizePolicy());
        mainBar.setSizePolicy(expandingSizePolicy());
        mainBar.addWidget(area);

        QToolBar buttons1 = new QToolBar();
        buttons1.setOrientation(Qt.Orientation.Horizontal);

        QPushButton addColumnButton = new QPushButton("+");
        addColumnButton.clicked.connect(this::addClicked);
        buttons1.addWidget(addColumnButton);

        QPushButton removeColumnButton = new QPushButton("-");
        removeColumnButton.clicked.connect(this::removeClicked);
        buttons1.addWidget(removeColumnButton);

        mainBar.addWidget(buttons1);

        QToolBar buttons = new QToolBar();
        QPushButton create = new QPushButton("Create");
        QPushButton cancel = new QPushButton("Cancel");
        create.clicked.connect(this::create);
        cancel.clicked.connect(this::cancelClicked);
        buttons.setOrientation(Qt.Orientation.Horizontal);
        buttons.addWidget(create);
        buttons.addWidget(cancel);
        mainBar.addWidget(buttons);

        setLayoutAndShow(mainBar);
        show();

    }

    private void addColumnLine(boolean first) {
        if (!first) {
            columns.setRowCount(columns.rowCount() + 1);
        }
        QTableWidgetItem columnName = new QTableWidgetItem();
        TypeButton type = new TypeButton();
        QCheckBox nullable = new QCheckBox();
        QCheckBox prKey = new QCheckBox();
        QTableWidgetItem defaultValue = new QTableWidgetItem();
        QCheckBox fKey = new QCheckBox();
        TypeButton fColumn = new TypeButton(fColumns);
        TypeButton fTable = new TypeButton(tables, fColumn);


        var currentRow = columns.rowCount() - 1;
        columns.setItem(currentRow, 0, columnName);
        columns.setCellWidget(currentRow, 1, type);
        columns.setCellWidget(currentRow, 2, prKey);
        columns.setCellWidget(currentRow, 3, nullable);
        columns.setItem(currentRow, 4, defaultValue);
        columns.setCellWidget(currentRow, 5, fKey);
        columns.setCellWidget(currentRow, 6, fTable);
        columns.setCellWidget(currentRow, 7, fColumn);
    }

    void addClicked() {
        addColumnLine(false);
        updateAll();
    }

    private void updateAll() {
        columns.update();
        area.update();
        mainBar.update();
        Objects.requireNonNull(layout()).update();
    }

    void removeClicked() {
        columns.removeRow(columns.rowCount() - 1);
    }

    private List<String> titles() {
        List<String> titles = new ArrayList<>();
        titles.add("Column name");
        titles.add("Type");
        titles.add("Primary key");
        titles.add("Not null");
        titles.add("Default value");
        titles.add("Foreign key");
        titles.add("Parent table");
        titles.add("Parent column");
        return titles;
    }

    private void create() {
        String name = this.tableName.toPlainText();
        boolean flag = true;
        if (name.equals("")) {
            new ErrorDialog("Empty table name");
            flag = false;
        }
        if (name.contains(" ")) {
            new ErrorDialog("Wrong table name format. Use '_' instead of whitespaces");
            flag = false;
        }

        if (flag) {
            var con = StorageController.connectionStorage.getConnection(controller.root.connectionStorageView.getCurrentConnection());
            con.newTable(name, "desk");

            for (int i = 0; i < columns.rowCount(); i++) {
                String columnName = Objects.requireNonNull(columns.item(i, 0)).text();
                if (columnName.equals("")) {
                    new ErrorDialog("Empty column name");
                    break;
                }
                if (columnName.contains(" ")) {
                    new ErrorDialog("Wrong column name format. Use '_' instead of whitespaces");
                    break;
                }

                var typeButton = (QPushButton) columns.cellWidget(i, 1);
                assert typeButton != null;
                String type = typeButton.text();
                var pKeyBox = (QCheckBox) columns.cellWidget(i, 2);
                assert pKeyBox != null;
                boolean pKey = pKeyBox.getChecked();
                var nullBox = (QCheckBox) columns.cellWidget(i, 3);
                assert nullBox != null;
                boolean notNull = nullBox.getChecked();
                String defaultValue = Objects.requireNonNull(columns.item(i, 4)).text();
                var fBox = (QCheckBox) columns.cellWidget(i, 5);
                assert fBox != null;
                boolean fKey = fBox.getChecked();
                var fTableB = (QPushButton) columns.cellWidget(i, 6);
                assert fTableB != null;
                String fTable = fTableB.text();
                var fColumnB = (QPushButton) columns.cellWidget(i, 7);
                assert fColumnB != null;
                String fColumn = fColumnB.text();

                con.getSchema().getTable(name).addColumn(columnName, translate(type), notNull, defaultValue);

                if (pKey) {
                    con.getSchema().getTable(name).addKey(name + "_pkey", new ArrayList<>(List.of(columnName)));
                }
                if (fKey) {
                    con.getSchema().getTable(name).addForeignKey(name + "_fkey", new ArrayList<>(List.of(columnName)), fTable, new ArrayList<>(List.of(fColumn)), "");
                }
            }

            con.createTable(name);
            con.saveChanges();
            this.close();
            controller.loadDatasources();
        }
//        con.getSchema().getTable("amina")
//            .addIndex("boarding_passes_flight_id_boarding_no_key2", true, new ArrayList<>(List.of("flight_id", "boarding_no")));

    }


    private QSizePolicy expandingSizePolicy() {
        QSizePolicy sizePolicy = new QSizePolicy();
        sizePolicy.setVerticalPolicy(QSizePolicy.Policy.Expanding);
        sizePolicy.setHorizontalPolicy(QSizePolicy.Policy.Expanding);
        return sizePolicy;
    }

    private String translate(String type) {
        return switch (type) {
            case "Text" -> "text";
            case "Integer" -> "int4";
            case "Time" -> "timestamptz";
            case "Json" -> "jsonb";
            default -> "text";
        };
    }
}
