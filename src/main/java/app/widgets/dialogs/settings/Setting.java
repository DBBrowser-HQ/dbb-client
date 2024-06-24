package app.widgets.dialogs.settings;

import io.qt.core.QMetaObject;
import io.qt.widgets.QDialog;
import io.qt.widgets.QGridLayout;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;

public abstract class Setting extends QDialog {

    protected final Signal1<String> mainSignal = new Signal1<>();


    protected QPushButton newButton(String text, QMetaObject.Slot0 signal) {
        QPushButton button = new QPushButton(text);
        button.clicked.connect(signal);
        return button;
    }

    protected void cancelClicked() {
        this.close();
    }

    protected void setLayoutAndShow(QWidget body) {
        QGridLayout layout = new QGridLayout();
        layout.addWidget(body);
        this.setLayout(layout);
        this.show();
    }

}
