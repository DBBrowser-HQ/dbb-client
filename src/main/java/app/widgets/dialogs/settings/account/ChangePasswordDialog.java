package app.widgets.dialogs.settings.account;

import app.api.ApiCalls;
import app.widgets.MyToolBar;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.Setting;
import io.qt.core.Qt;
import io.qt.widgets.*;

public class ChangePasswordDialog extends Setting {

    public QTextEdit passwordOld;
    public QTextEdit passwordNew;

    public ChangePasswordDialog() {

        QToolBar mainBar = new QToolBar();
        initInputs();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        mainBar.addWidget(new QLabel("Enter your old password"));
        mainBar.addWidget(passwordOld);
        mainBar.addWidget(new QLabel("Enter new password"));
        mainBar.addWidget(passwordNew);
        MyToolBar buttonsBar = new MyToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        buttonsBar.addWidgetAndSeparator(newButton("Cancel", this::cancelClicked));
        buttonsBar.addWidget(newButton("Approve", this::approveClicked));
        mainBar.addWidget(buttonsBar);
        setLayoutAndShow(mainBar);
    }

    private void initInputs() {
        passwordOld = new QTextEdit();
        passwordOld.setText("");
        passwordOld.setMaximumHeight(27);
        passwordNew = new QTextEdit();
        passwordNew.setText("");
        passwordNew.setMaximumHeight(27);
    }

    void approveClicked() {
        if (!passwordOld.toPlainText().equals("") && !passwordNew.toPlainText().equals("")) {
            //ApiCalls.changePassword();
            this.close();
        }
        else {
            new ErrorDialog("Empty input");
        }
    }

}
