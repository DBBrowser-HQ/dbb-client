package app;

import io.qt.gui.QIcon;
import io.qt.gui.QPixmap;

import java.io.IOException;
import java.util.Objects;

public class IconLoader {

    /**
     * иконки загружаем через этот класс.
     * Можно подумать над параметризацией конструктора, или
     * захардкодить методы, которые возвращают разные иконки для разных нужд.
     * -
     * Logo icon by Igor Zhuravskiy
     */
    public QIcon loadIcon(String name) throws IOException {
        QPixmap pixmap = new QPixmap();
        pixmap.loadFromData(Objects.requireNonNull(this.getClass().getResourceAsStream(name)).readAllBytes());
        QIcon icon = new QIcon();
        icon.addPixmap(pixmap);
        return icon;
    }

    public static QIcon loadIconStatic(String name) throws IOException {
        QPixmap pixmap = new QPixmap();
        pixmap.loadFromData(Objects.requireNonNull(IconLoader.class.getResourceAsStream(name)).readAllBytes());
        QIcon icon = new QIcon();
        icon.addPixmap(pixmap);
        return icon;
    }

}
