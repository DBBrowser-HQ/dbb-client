package app.widgets.dialogs.settings.users;

import io.qt.gui.QAction;
import io.qt.widgets.QPushButton;

public class AddUserAction extends QAction {

    QPushButton root;

    public AddUserAction(String text, QPushButton root) {
        this.setText(text);
        this.root = root;
        this.triggered.connect(this, "clicked()");
    }

    void clicked() {
        root.setText(this.getText());
    }

}
