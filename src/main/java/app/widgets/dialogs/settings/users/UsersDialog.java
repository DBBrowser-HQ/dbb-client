package app.widgets.dialogs.settings.users;

import app.api.ApiCalls;
import app.api.data.responses.User;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.ApproveDeleteDialog;
import io.qt.core.Qt;
import io.qt.gui.QAction;
import io.qt.gui.QCursor;
import io.qt.gui.QStandardItem;
import io.qt.gui.QStandardItemModel;
import io.qt.widgets.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UsersDialog extends QDialog {

    private final Signal1<List<User>> signalOk = new Signal1<>();
    private final Signal1<String> signalErr = new Signal1<>();
    private final QTableView users = new QTableView();
    private QMenu contextMenu;
    private final int orgId;

    public UsersDialog(int orgId) {
        this.orgId = orgId;
        prepareSignals();
        initContextMenu();
        QGridLayout layout = new QGridLayout();
        users.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        users.customContextMenuRequested.connect(this, "contextMenuRequested()");
        users.setSizePolicy(expandingSizePolicy());
        users.setShowGrid(true);
        layout.addWidget(users);
        QPushButton addUser = new QPushButton("Invite user");
        addUser.clicked.connect(this, "addUserClicked()");
        layout.addWidget(addUser);
        QPushButton cancel = new QPushButton("Cancel");
        cancel.clicked.connect(this, "cancelClicked()");
        layout.addWidget(cancel);
        this.setLayout(layout);
        getUsers();
        this.show();
    }

    void addUserClicked() {
        new AddUserDialog(this, orgId);
    }

    void cancelClicked() {
        this.close();
    }

    private void prepareSignals() {
        signalOk.connect(this, "showUsers(List)");
        signalErr.connect(this, "error(String)");
    }

    void error(String error) {
        new ErrorDialog(error);
    }

    void contextMenuRequested() {
        contextMenu.popup(QCursor.pos());
    }

    private void initContextMenu() {
        contextMenu = new QMenu();

        QAction connect = new QAction("Change role");
        connect.triggered.connect(this, "changeRole()");
        contextMenu.addAction(connect);

        QAction rename = new QAction("Delete");
        rename.triggered.connect(this, "deleteUser()");
        contextMenu.addAction(rename);
    }

    void changeRole() {
        new ChangeUserRoleDialog(this, orgId, Objects.requireNonNull(Objects.requireNonNull(users.model()).data(users.currentIndex().row(), 1)).toString(), getCurrentId());
    }

    void deleteUser() {
        new ApproveDeleteDialog(this, "user from organization", () -> ApiCalls.deleteUser(this::getUsers, signalErr, getCurrentId(), orgId));
    }

    void callback(String res) {
        if (!Objects.equals(res, "OK")) {
            new ErrorDialog(res);
        }
        getUsers();
    }

    private void getUsers() {
        ApiCalls.getUsersInOrganization(signalOk, signalErr, orgId);
    }

    void showUsers(List<User> users1) {

        QStandardItemModel data = new QStandardItemModel();
        List<String> labels = new ArrayList<>();
        labels.add("Login");
        labels.add("Role");
        labels.add("Id");
        data.setHorizontalHeaderLabels(labels);

        for (User u : users1) {
            List<QStandardItem> itemList = new ArrayList<>();
            var n = new QStandardItem(u.login);
            n.setEditable(false);
            itemList.add(n);
            var r = new QStandardItem(u.role);
            r.setEditable(false);
            itemList.add(r);
            var i = new QStandardItem(String.valueOf(u.id));
            i.setEditable(false);
            itemList.add(i);
            data.appendRow(itemList);
        }

        users.setModel(data);
    }

    private QSizePolicy expandingSizePolicy() {
        QSizePolicy sizePolicy = new QSizePolicy();
        sizePolicy.setVerticalPolicy(QSizePolicy.Policy.Expanding);
        sizePolicy.setHorizontalPolicy(QSizePolicy.Policy.Expanding);
        return sizePolicy;
    }

    void added(String res) {
        if (res.equals("OK")) {
            getUsers();
        }
        else {
            new ErrorDialog(res);
        }
    }

    private int getCurrentId() {
        return Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(users.model()).data(users.currentIndex().row(), 2)).toString());
    }

}
