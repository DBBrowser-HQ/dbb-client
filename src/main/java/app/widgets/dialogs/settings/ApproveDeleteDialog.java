package app.widgets.dialogs.settings;

import io.qt.widgets.*;

import static io.qt.core.Qt.WindowType.WindowStaysOnTopHint;

public class ApproveDeleteDialog extends QDialog {

    private final QCheckBox checkBox;
    private final QWidget root;
    private final Runnable action;

    public ApproveDeleteDialog(QWidget root, String text, Runnable action) {
        this.action = action;
        this.root = root;
        this.finished.connect(this::enableControl);
        this.setWindowFlag(WindowStaysOnTopHint);
        QLabel label = new QLabel("Are you sure you want to delete " + text + "?");
        this.setWindowTitle("Approve action");
        checkBox = new QCheckBox();
        checkBox.setText("Yes");

        QPushButton applyButton = new QPushButton("Approve");
        applyButton.clicked.connect(this::applyClicked);

        QPushButton abortButton = new QPushButton("Abort");
        abortButton.clicked.connect(this::abortClicked);

        QLayout layout = new QGridLayout(this);
        layout.addWidget(label);
        layout.addWidget(checkBox);

        QToolBar buttonsBar = new QToolBar();
        buttonsBar.addWidget(applyButton);
        buttonsBar.addSeparator();
        buttonsBar.addWidget(abortButton);

        layout.addWidget(buttonsBar);
        this.setLayout(layout);
        this.open();
    }

    void applyClicked() {
        if (checkBox.getChecked()) {
            action.run();
        }
        this.close();
    }

    void abortClicked() {
        this.close();
    }

    void enableControl() {
        root.setEnabled(true);
    }

}