package app.widgets.dialogs.settings;

import app.api.ApiCalls;
import app.widgets.dialogs.ErrorDialog;
import app.widgets.dialogs.settings.orgs.CreateOrganizationDialog;
import io.qt.widgets.QWidget;

public class CreateDatasourceDialog extends CreateOrganizationDialog {

    private final int id;

    public CreateDatasourceDialog(QWidget root, int id) {
        super(root);
        mainSignal.connect(root, "updateData()");
        label.setText("Enter name for your datasource");
        setWindowTitle("Create new datasource");
        this.id = id;
    }

    void approveClicked() {
        if (!name.toPlainText().equals("")) {
            ApiCalls.createDataSource(mainSignal, id, name.toPlainText());
            this.close();
        }
        else {
            new ErrorDialog("Empty input");
        }
    }
}
