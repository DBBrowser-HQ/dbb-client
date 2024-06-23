package app.widgets.dialogs.start;

import app.MainWindow;
import app.widgets.dialogs.settings.Management;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.*;

public class SelectOrganizationDialog extends StartDialog {

    private final QIcon icon;

    public SelectOrganizationDialog(QIcon icon) {
        this.icon = icon;
        setWindowIcon(icon);
        QToolBar mainBar = new QToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        mainBar.addWidget(new QLabel("Select the organization or create a new one"));
        mainBar.addWidget(new Management(this::comeToMain));
        QPushButton cancelButton = newButton("Cancel", "cancelClicked()");
        mainBar.addWidget(cancelButton);
        mainBar.setSizePolicy(expandingSizePolicy());
        QGridLayout layout = new QGridLayout(this);
        layout.addWidget(mainBar);
        show();
    }

    private void comeToMain() {
        new MainWindow(icon, true);
        this.close();
    }

    private QSizePolicy expandingSizePolicy() {
        QSizePolicy sizePolicy = new QSizePolicy();
        sizePolicy.setVerticalPolicy(QSizePolicy.Policy.Expanding);
        sizePolicy.setHorizontalPolicy(QSizePolicy.Policy.Expanding);
        return sizePolicy;
    }

}
