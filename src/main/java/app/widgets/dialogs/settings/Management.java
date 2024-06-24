package app.widgets.dialogs.settings;

import app.api.ApiCalls;
import app.api.UserDataRepository;
import app.api.data.responses.UserOrganization;
import app.widgets.MyToolBar;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.orgs.CreateOrganizationDialog;
import app.widgets.dialogs.settings.orgs.RenameOrganizationDialog;
import app.widgets.dialogs.settings.users.AddUserDialog;
import app.widgets.dialogs.settings.users.UsersDialog;
import io.qt.core.QModelIndex;
import io.qt.core.QPair;
import io.qt.core.Qt;
import io.qt.gui.QAction;
import io.qt.gui.QCursor;
import io.qt.gui.QStandardItem;
import io.qt.gui.QStandardItemModel;
import io.qt.widgets.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Management extends MyToolBar {

    private final Signal1<List<UserOrganization>> signalOk = new Signal1<>();
    private final Signal1<String> signalErr = new Signal1<>();
    private final QTableView comps = new QTableView();
    private final Runnable callback;
    private QMenu contextMenu;

    public Management(Runnable callback) {
        this.callback = callback;
        prepareSignals();
        setOrientation(Qt.Orientation.Vertical);
        addWidget(new QLabel("Your companies:"));
        addWidget(getCompanies());
        QPushButton addCompanyButton = new QPushButton("+");
        addCompanyButton.clicked.connect(this::addCompanyClicked);
        addWidget(addCompanyButton);
        this.setSizePolicy(expandingSizePolicy());
        comps.doubleClicked.connect(this::connect1);
        comps.setSizePolicy(expandingSizePolicy());
        comps.setShowGrid(true);
        comps.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        comps.customContextMenuRequested.connect(this, "contextMenuRequested()");
        initContextMenu();
    }

    private QTableView getCompanies() {
        ApiCalls.getUserOrganizations(signalOk, signalErr);
        return comps;
    }

    private void prepareSignals() {
        signalOk.connect(this, "showCompanies(List)");
        signalErr.connect(this, "error(String)");
    }

    private void initContextMenu() {
        contextMenu = new QMenu();

        QAction connect = new QAction("Connect");
        connect.triggered.connect(this::connect);
        contextMenu.addAction(connect);

        QAction rename = new QAction("Rename");
        rename.triggered.connect(this::rename);
        contextMenu.addAction(rename);

        QAction createDatasource = new QAction("Create datasource");
        createDatasource.triggered.connect(this::createDatasource);
        contextMenu.addAction(createDatasource);

//        QAction deleteDatasource = new QAction("Delete datasource");
//        createDatasource.triggered.connect(this, "deleteDatasource()");
//        contextMenu.addAction(deleteDatasource);

        QAction addUser = new QAction("Add user");
        addUser.triggered.connect(this::addUser);
        contextMenu.addAction(addUser);

        QAction users = new QAction("Manage users");
        users.triggered.connect(this::users);
        contextMenu.addAction(users);

        QAction deleteOrg = new QAction("Delete organization");
        deleteOrg.triggered.connect(this::deleteOrganization);
        contextMenu.addAction(deleteOrg);
    }

    void contextMenuRequested() {
        contextMenu.popup(QCursor.pos());
    }

    void connect() {
        connect1(this.comps.currentIndex());
    }

    void rename() {
        new RenameOrganizationDialog(this, getCurrentId());
    }

    void createDatasource() {
        new CreateDatasourceDialog(this, getCurrentId());
    }

    void users() {
        new UsersDialog(getCurrentId());
    }

    void renamed() {

    }

    void deleteOrganization() {
        new ApproveDeleteDialog(this, "this organization", () -> ApiCalls.deleteOrganization(getCurrentId(), callback));
    }

    private int getCurrentId() {
        return Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(comps.model()).data(comps.currentIndex().row(), 2)).toString());
    }

    void addUser() {
        new AddUserDialog(this, getCurrentId());
    }

    void deleteCompany() {

    }

    void showCompanies(List<UserOrganization> organizations) {

        QStandardItemModel data = new QStandardItemModel();
        List<String> labels = new ArrayList<>();
        labels.add("Organization");
        labels.add("Your role");
        labels.add("Id");
        data.setHorizontalHeaderLabels(labels);

        for (var org : organizations) {

            if (UserDataRepository.currentCompany != null && org.id == UserDataRepository.currentCompany.first) {
                UserDataRepository.currentCompany.second = org.name;
            }

            List<QStandardItem> itemList = new ArrayList<>();
            var n = new QStandardItem(org.name);
            n.setEditable(false);
            itemList.add(n);
            var r = new QStandardItem(org.role);
            r.setEditable(false);
            itemList.add(r);
            var i = new QStandardItem(String.valueOf(org.id));
            i.setEditable(false);
            itemList.add(i);
            data.appendRow(itemList);

        }
        comps.setModel(data);
    }

    void connect1(QModelIndex index) {
        try {
            UserDataRepository.currentCompany = new QPair<>(getCurrentId(), comps.model().data(index.row(), 0).toString());
        }
        catch (NullPointerException e) {
            System.err.println("Something went wrong");
        }
        callback.run();
    }

    void updateData() {
        callback.run();
    }

    void error(String error) {
        new ErrorDialog(error);
    }

    void addCompanyClicked() {
        new CreateOrganizationDialog(this);
    }

    void added(String res) {
        if (res.equals("OK")) {
            getCompanies();
        }
        else {
            new ErrorDialog(res);
        }
    }

    private QSizePolicy expandingSizePolicy() {
        QSizePolicy sizePolicy = new QSizePolicy();
        sizePolicy.setVerticalPolicy(QSizePolicy.Policy.Expanding);
        sizePolicy.setHorizontalPolicy(QSizePolicy.Policy.Expanding);
        return sizePolicy;
    }

}
