package app.widgets.dialogs.settings.users;

import app.api.ApiCalls;
import app.widgets.MyToolBar;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.Setting;
import io.qt.core.Qt;
import io.qt.widgets.*;

public class ChangeUserRoleDialog extends Setting {

    private final int orgId;
    private QTextEdit userId;
    private final QPushButton userRole;


    public ChangeUserRoleDialog(QWidget root, int orgId, String currentRole, int uId) {

        mainSignal.connect(root, "callback(String)");
        setWindowTitle("Select new role");
        initInputs(uId);
        QToolBar mainBar = new QToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        mainBar.addWidget(new QLabel("User id:"));
        mainBar.addWidget(userId);
        userRole = new QPushButton(currentRole);
        QMenu popMenu = new QMenu(currentRole);
        popMenu.addAction(new AddUserAction("reader", userRole));
        popMenu.addAction(new AddUserAction("redactor", userRole));
        popMenu.addAction(new AddUserAction("admin", userRole));
        userRole.setMenu(popMenu);
        mainBar.addWidget(userRole);
        MyToolBar buttonsBar = new MyToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        buttonsBar.addWidgetAndSeparator(newButton("Submit", this::approveClicked));
        buttonsBar.addWidget(newButton("Cancel", this::cancelClicked));
        mainBar.addWidget(buttonsBar);
        setLayoutAndShow(mainBar);

        this.orgId = orgId;
    }

    private void initInputs(int id) {
        userId = new QTextEdit();
        userId.setText(String.valueOf(id));
        userId.setMaximumHeight(27);
    }

    void approveClicked() {
        if (!userId.toPlainText().equals("")) {
            try {
                ApiCalls.changeUserRole(mainSignal, mainSignal, Integer.parseInt(userId.toPlainText()), orgId, userRole.getText());
            }
            catch (NumberFormatException e) {
                new ErrorDialog("Wrong id format");
            }
            this.close();
        }
        else {
            new ErrorDialog("Empty input");
        }
    }

}
