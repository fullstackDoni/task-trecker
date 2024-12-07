import service.file.FileBackedTaskManager;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        File file = new File("temporary.csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try {
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

            System.out.println("Загруженные задачи:");
            loadedManager.getAllTasks().forEach(System.out::println);

            System.out.println("Загруженные эпики:");
            loadedManager.getAllEpics().forEach(System.out::println);

            System.out.println("Загруженные подзадачи:");
            loadedManager.getAllSubtasks().forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("Ошибка при загрузке данных: " + e.getMessage());
        }
    }
}
