package app.widgets.dialogs.start;

import app.MainWindow;
import app.api.ApiCalls;
import app.backend.controllers.StorageController;
import app.styles.WoodPallet;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.InputItem;
import app.widgets.dialogs.settings.Management;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OnlineStartDialog extends StartDialog {

    private final Signal1<String> signal1 = new Signal1<>();

    public OnlineStartDialog(QIcon icon) {
        StorageController.init();

        this.icon = icon;
        this.setWindowIcon(icon);
        //this.setStyle(new QCommonStyle());
        //this.setPalette(new WoodPallet());
        QLabel label = new QLabel("Please sign-in or switch to offline mode");
        this.setWindowTitle("Welcome to DB browser");

        QToolBar buttonsBar = new QToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        QPushButton connectButton = newButton("Sign-in", "connectClicked()");
        QPushButton cancelButton = newButton("Cancel", "cancelClicked()");
        QPushButton continueButton = newButton("Work offline", "skip()");
        QPushButton registerButton = newButton("Sign-up", "signUp()");

        buttonsBar.addWidget(connectButton);
        buttonsBar.addWidget(new QSplitter());
        buttonsBar.addWidget(registerButton);
        buttonsBar.addWidget(new QSplitter());
        buttonsBar.addWidget(continueButton);
        buttonsBar.addWidget(new QSplitter());
        buttonsBar.addWidget(cancelButton);

        QLayout layout = new QGridLayout();
        layout.addWidget(label);
        QToolBar logInTab = logInTab();
        layout.addWidget(logInTab);
        layout.addWidget(buttonsBar);
        setLayout(layout);
        this.show();

    }

    private void initInputs() {
        login = new QTextEdit();
        login.setMaximumHeight(27);
        password = new QTextEdit();
        password.setMaximumHeight(27);
    }

    private QToolBar logInTab() {

        initInputs();

        List<QToolBar> toolBars = new ArrayList<>(4);
        toolBars.add(new InputItem("login:  ", login));
        toolBars.add(new InputItem("password:  ", password));

        QToolBar logInBar = new QToolBar();
        logInBar.setOrientation(Qt.Orientation.Vertical);

        for (QToolBar toolBar : toolBars) {
            logInBar.addWidget(toolBar);
        }

        return logInBar;
    }

    void connectClicked() {

        if (!login.toPlainText().equals("") && !password.toPlainText().equals("")) {

            signal1.connect(this, "result(String)");
            ApiCalls.signIn(signal1, login.toPlainText(), password.toPlainText());
        }

    }

    void skip() {
        this.close();
        new OfflineStartDialog(this.icon);
    }

    private void comeToMain() {
        this.close();
        new SelectOrganizationDialog(icon);
    }

    void result(String res) {
        if(Objects.equals(res, "OK")) {
            comeToMain();
        }
        else {
            new ErrorDialog(res);
        }
    }

    void signUp() {
        SignUpDialog sign = new SignUpDialog(icon);
        this.close();
    }

}
