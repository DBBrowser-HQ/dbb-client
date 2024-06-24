package app.widgets.dialogs.settings.users;

import io.qt.QNoSuchSlotException;
import io.qt.gui.QAction;
import io.qt.widgets.QPushButton;

public class AddUserAction extends QAction {

    private final QPushButton root;

    public AddUserAction(String text, QPushButton root) {
        this.setText(text);
        this.root = root;
        this.triggered.connect(this, "clicked()");
        try {
            this.triggered.connect(root, "selected()");
        } catch (QNoSuchSlotException ignored) {
        }
    }

    void clicked() {
        root.setText(this.getText());
    }

}
