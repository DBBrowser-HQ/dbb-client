package app.widgets.dialogs;

import app.widgets.dialogs.settings.users.AddUserAction;
import io.qt.widgets.QMenu;
import io.qt.widgets.QPushButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeButton extends QPushButton {

    private final Signal1<String> selectedSignal = new Signal1<>();
    private Map<String, List<String>> fColumns;

    public TypeButton() {
        QMenu menu = new QMenu();
        menu.addAction(new AddUserAction("Integer", this));
        menu.addAction(new AddUserAction("Text", this));
        menu.addAction(new AddUserAction("Time", this));
        menu.addAction(new AddUserAction("Json", this));
        setMenu(menu);
    }

    public TypeButton(List<String> tables, TypeButton child) {
        QMenu menu = new QMenu();
        for (var t : tables) {
            menu.addAction(new AddUserAction(t, this));
        }
        setMenu(menu);
        selectedSignal.connect(child::getSelected);
    }

    private void selected() {
        selectedSignal.emit(this.getText());
    }

    private void getSelected(String table) {
        QMenu menu = new QMenu();
        for (var t : fColumns.get(table)) {
            menu.addAction(new AddUserAction(t, this));
        }
        setMenu(menu);
        update();
    }

    public TypeButton(Map<String, List<String>> fColumns) {
        this.fColumns = fColumns;
    }

}
