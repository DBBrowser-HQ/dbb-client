package app.widgets.dialogs.start;

import app.MainWindow;
import app.api.ApiCalls;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.InputItem;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignUpDialog extends StartDialog {

    private final Signal1<String> signal1 = new Signal1<>();

    public SignUpDialog(QIcon icon) {
        this.icon = icon;
        this.setWindowIcon(icon);
        QLayout layout = new QGridLayout( this );
        this.setWindowTitle("Sign up");

        QToolBar buttonsBar = new QToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        QPushButton connectButton = newButton("Sign-up", "connectClicked()");
        QPushButton cancelButton = newButton("Cancel", "cancelClicked()");

        buttonsBar.addWidget(connectButton);
        buttonsBar.addWidget(new QSplitter());
        buttonsBar.addWidget(cancelButton);

        QToolBar logInTab = logInTab();
        layout.addWidget(logInTab);

        layout.addWidget(buttonsBar);

        this.show();
    }

    void result(String res) {
        if(Objects.equals(res, "OK")) {
            signal1.disconnect(this);
            signal1.connect(this, "callback(String)");
            ApiCalls.signIn(signal1, login.toPlainText(), password.toPlainText());
        }
        else {
            new ErrorDialog(res);
        }
    }

    void callback(String res) {
        if(Objects.equals(res, "OK")) {
            comeToMain();
        }
        else {
            new ErrorDialog(res);
        }
    }

    private void comeToMain() {
        this.close();
        new SelectOrganizationDialog(this.icon);
    }

    void connectClicked() {

        if (!login.toPlainText().equals("") && !password.toPlainText().equals("")) {
            signal1.connect(this, "result(String)");
            ApiCalls.signUp(signal1, login.toPlainText(), password.toPlainText());
        }

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

    private void initInputs() {
        login = new QTextEdit();
        login.setMaximumHeight(27);
        password = new QTextEdit();
        password.setMaximumHeight(27);
    }


}
