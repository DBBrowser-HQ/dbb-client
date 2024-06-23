package app.widgets.dialogs.settings;

import app.MenuController;
import app.api.UserDataRepository;
import app.widgets.dialogs.settings.account.AccountSettings;
import app.widgets.dialogs.start.OnlineStartDialog;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.*;

public class SettingsDialog extends Setting {

    private final MenuController controller;

    public SettingsDialog(QIcon icon, Runnable callback, MenuController controller) {
        this.controller = controller;
        this.setParent(controller.root, Qt.WindowType.Dialog);
        setWindowIcon(icon);
        setWindowTitle("Settings");
        QTabWidget tabs = new QTabWidget();
        QToolBar accountTab = new AccountSettings(() -> {this.close(); controller.close();
            UserDataRepository.logOut(); new OnlineStartDialog(windowIcon());
        });
        Management managementTab = new Management(() -> {callback.run(); this.close();});
        tabs.addTab(accountTab, "Account");
        tabs.addTab(managementTab, "Management");
        QPushButton cancelButton = newButton("Cancel", "cancelClicked()");
        QToolBar mainBar = new QToolBar();
        mainBar.setOrientation(Qt.Orientation.Vertical);
        mainBar.addWidget(tabs);
        QToolBar buttonsBar = new QToolBar();
        buttonsBar.setOrientation(Qt.Orientation.Horizontal);
        buttonsBar.addWidget(new QSplitter());
        buttonsBar.addWidget(cancelButton);
        mainBar.addWidget(buttonsBar);
        this.setLayoutAndShow(mainBar);
        this.finished.connect(this, "fullUpdate()");
    }

    void fullUpdate() {
        // TODO logic
        controller.updateRoot(true);
    }

}
