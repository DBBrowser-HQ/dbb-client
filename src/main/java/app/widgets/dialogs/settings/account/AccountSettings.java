package app.widgets.dialogs.settings.account;

import app.api.UserDataRepository;
import app.widgets.MyToolBar;
import app.widgets.dialogs.InputItem;
import io.qt.core.Qt;
import io.qt.widgets.*;

import java.util.ArrayList;
import java.util.List;

public class AccountSettings extends MyToolBar {

    public QTextEdit login;
    public QTextEdit password;
    private Runnable rootClose;

    public AccountSettings(Runnable rootClose) {

        this.rootClose = rootClose;
        this.addWidget(logInTab());

    }

    private void initInputs() {
        login = new QTextEdit();
        login.setText(UserDataRepository.userData.login);
        login.setReadOnly(true);
        login.setMaximumHeight(27);
        password = new QTextEdit();
        password.setReadOnly(true);
        password.setText(convertPassword());
        password.setMaximumHeight(27);
    }

    private String convertPassword() {
        return "*".repeat(UserDataRepository.userData.password.length());
    }

    private QToolBar logInTab() {

        initInputs();

        List<QWidget> toolBars = new ArrayList<>(4);
        toolBars.add(new QLabel("Id: " + UserDataRepository.getUserId()));
        toolBars.add(new InputItem("login:  ", login));
        toolBars.add(new InputItem("password:  ", password));
        QPushButton changePasswordButton = new QPushButton("Change password");
        changePasswordButton.clicked.connect(this, "changePassword()");
        QPushButton logOutButton = new QPushButton("Log out");
        logOutButton.clicked.connect(this, "logOut()");

        QToolBar loginPasswordInBar = new QToolBar();
        loginPasswordInBar.setOrientation(Qt.Orientation.Vertical);

        for (QWidget toolBar : toolBars) {
            loginPasswordInBar.addWidget(toolBar);
        }
        loginPasswordInBar.addWidget(changePasswordButton);
        loginPasswordInBar.addWidget(logOutButton);

        return loginPasswordInBar;
    }

    void changePassword() {
        new ChangePasswordDialog();
    }

    void logOut() {
        rootClose.run();
    }

}
