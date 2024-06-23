package app.backend.utility;

import app.backend.entities.ConnectionStorage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Saver {
    public static void saveConnectionStorage(ConnectionStorage connectionStorage){
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream("saves/save.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(connectionStorage);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static ConnectionStorage getConnectionStorage() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("saves/save.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            return (ConnectionStorage) objectInputStream.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

    }

}