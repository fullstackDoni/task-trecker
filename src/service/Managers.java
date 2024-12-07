package service;
import service.file.FileBackedTaskManager;
import service.managers.HistoryManager;
import service.managers.InMemoryHistoryManager;
import service.managers.InMemoryTaskManager;
import service.managers.TaskManager;
import java.io.File;

public class Managers {

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }

    public static TaskManager getFileBackedTaskManager(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Имя файла не должно быть пустым или null");
        }
        return new FileBackedTaskManager(new File(fileName));
    }
}
