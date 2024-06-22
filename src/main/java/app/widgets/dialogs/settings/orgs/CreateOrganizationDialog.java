package app.widgets.dialogs.settings.orgs;

import app.api.ApiCalls;
import app.widgets.MyToolBar;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.Setting;
import io.qt.core.Qt;
import io.qt.widgets.*;

public class CreateOrganizationDialog extends Setting {

    public QTextEdit name;
    protected QLabel label;

    public CreateOrganizationDialog(QWidget root) {
        mainSignal.connect(root, "added(String)");
        setWindowTitle("Create new organization");
        initInputs();
        QToolBar mainBar = new QToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        label = new QLabel("Enter name for your organization");
        mainBar.addWidget(label);
        mainBar.addWidget(name);
        MyToolBar buttonsBar = new MyToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        buttonsBar.addWidgetAndSeparator(newButton("Submit", "approveClicked()"));
        buttonsBar.addWidget(newButton("Cancel", "cancelClicked()"));
        mainBar.addWidget(buttonsBar);
        setLayoutAndShow(mainBar);
    }

    private void initInputs() {
        name = new QTextEdit();
        name.setText("");
        name.setMaximumHeight(27);
    }

    void approveClicked() {
        if (!name.toPlainText().equals("")) {
            ApiCalls.createOrganization(mainSignal, name.toPlainText());
            this.close();
        }
        else {
            new ErrorDialog("Empty input");
        }
    }

}
