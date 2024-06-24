package app.widgets.dialogs;

import app.widgets.dialogs.settings.users.AddUserAction;
import io.qt.widgets.QMenu;
import io.qt.widgets.QPushButton;

public class TypeButton extends QPushButton {

    public TypeButton() {
        QMenu menu = new QMenu();
        menu.addAction(new AddUserAction("Integer", this));
        menu.addAction(new AddUserAction("Text", this));
        menu.addAction(new AddUserAction("Time", this));
        menu.addAction(new AddUserAction("Json", this));
        setMenu(menu);
    }

}
