import app.Config;
import app.IconLoader;
import app.widgets.dialogs.start.OnlineStartDialog;
import io.qt.widgets.QApplication;

import java.io.*;
import java.nio.file.NoSuchFileException;

public class Main {

    public static void main( String[] args ) throws IOException {

        setConfigs();

        // Создаём Qt приложение, чтобы инициализировать библиотеку
        // и графическую подсистему.

        QApplication app = QApplication.initialize(args);

        IconLoader iconLoader = new IconLoader();
        OnlineStartDialog onlineStartDialog = new OnlineStartDialog(iconLoader.loadIcon("../icon.ico"));

        QApplication.exec();

        QApplication.shutdown();

    }

    private static void setConfigs() throws NoSuchFileException {
        try {
            FileReader fr = new FileReader("config.txt");
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (line != null) {
                String[] triple = line.split(" ");
                switch (triple[0]) {
                    case "backendUrl" -> Config.backendUrl = triple[2];
                    case "host" -> Config.host = triple[2];
                    case "proxyPort" -> Config.proxyPort = triple[2];
                    case "filePath" -> Config.filePath = triple[2];
                }
                line = br.readLine();
            }
        } catch (NullPointerException e) {
            throw new NoSuchFileException("Error with config file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}


