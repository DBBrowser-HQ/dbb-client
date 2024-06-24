package app.widgets.dialogs.settings.users;

import app.api.ApiCalls;
import app.widgets.MyToolBar;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.Setting;
import io.qt.core.Qt;
import io.qt.widgets.*;

public class AddUserDialog extends Setting {

    private final int orgId;
    private QTextEdit userId;
    private final QPushButton userRole;


    public AddUserDialog(QWidget root, int orgId) {

        mainSignal.connect(root, "added(String)");
        setWindowTitle("Add user to organization");
        initInputs();
        QToolBar mainBar = new QToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        QLabel label = new QLabel("Enter user id");
        mainBar.addWidget(label);
        mainBar.addWidget(userId);
        userRole = new QPushButton("reader");
        QMenu popMenu = new QMenu("reader");
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

    private void initInputs() {
        userId = new QTextEdit();
        userId.setText("");
        userId.setMaximumHeight(27);
    }

    void approveClicked() {
        if (!userId.toPlainText().equals("")) {
            try {
                ApiCalls.addUser(Integer.parseInt(userId.toPlainText()), orgId, userRole.getText(), mainSignal);
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
