package app.widgets.dialogs.settings.orgs;

import app.api.ApiCalls;
import app.widgets.dialogs.ErrorDialog;
import io.qt.widgets.QWidget;

public class RenameOrganizationDialog extends CreateOrganizationDialog{

    private final int id;

    public RenameOrganizationDialog(QWidget root, int id) {
        super(root);
        mainSignal.connect(root, "renamed()");
        this.id = id;
        setWindowTitle("Rename organization");
    }

    @Override
    void approveClicked() {
        if (!name.toPlainText().equals("")) {
            ApiCalls.renameOrganization(mainSignal, id, name.toPlainText());
            this.close();
        }
        else {
            new ErrorDialog("Empty input");
        }
    }
}
