package service;

import exception.ManagerSaveException;
import model.Epic;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.file.FileBackedTaskManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    private File tempFile;

    @Override
    protected FileBackedTaskManager createTaskManager() {
        return new FileBackedTaskManager(tempFile);
    }

    @BeforeEach
    void setUp() {
        tempFile = new File("temporary.csv");
        FileBackedTaskManager manager = createTaskManager();
        super.setUp();
    }

    @Test
    void shouldSaveAndLoadTaskFromFile() {
        Task task = new Task(1, "Task 1", "Description 1", Status.NEW, LocalDateTime.of(2000, 12, 3, 4, 5, 6), Duration.ofMinutes(2));
        taskManager.addTask(task);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Task loadedTask = loadedManager.getTask(task.getId());

        assertNotNull(loadedTask, "Задача должна быть восстановлена из файла.");
        assertEquals(task.getId(), loadedTask.getId(), "ID задачи не совпадает.");
        assertEquals(task.getName(), loadedTask.getName(), "Название задачи не совпадает.");
        assertEquals(task.getDescription(), loadedTask.getDescription(), "Описание задачи не совпадает.");
        assertEquals(task.getStatus(), loadedTask.getStatus(), "Статус задачи не совпадает.");
        assertEquals(task.getStartTime(), loadedTask.getStartTime(), "Время начала задачи не совпадает.");
        assertEquals(task.getDuration(), loadedTask.getDuration(), "Продолжительность задачи не совпадает.");
    }


    @Test
    void shouldSaveAndLoadEpicFromFile() {
        Epic epic = new Epic(1, "Epic 1", "Description 1", Status.NEW, Duration.ofMinutes(2));
        taskManager.addEpic(epic);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Epic loadedEpic = loadedManager.getEpic(epic.getId());

        assertEquals(epic.getId(), loadedEpic.getId(), "ID эпика должен совпадать.");
        assertEquals(epic.getName(), loadedEpic.getName(), "Имя эпика должно совпадать.");
        assertEquals(epic.getDescription(), loadedEpic.getDescription(), "Описание эпика должно совпадать.");
        assertEquals(epic.getStatus(), loadedEpic.getStatus(), "Статус эпика должен совпадать.");
        assertEquals(epic.getStartTime(), loadedEpic.getStartTime(), "Начальное время эпика должно совпадать.");
        assertEquals(epic.getEndTime(), loadedEpic.getEndTime(), "Конечное время эпика должно совпадать.");
        assertEquals(epic.getDuration(), loadedEpic.getDuration(), "Длительность эпика должна совпадать.");
    }
    @Test
    void shouldNotLoadTaskWithInvalidData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("id,type,name,description,status,startTime,duration,endTime,epicId\n");
            writer.write("1,INVALID_TYPE,Task 1,Description 1,NEW,2000-12-03T04:05:06,PT2M,,\n");
        } catch (IOException e) {
            fail("Не удалось записать тестовые данные в файл.");
        }

        assertThrows(ManagerSaveException.class, () -> {
            FileBackedTaskManager.loadFromFile(tempFile);
        }, "Менеджер должен выбросить исключение при попытке загрузить некорректные данные.");
    }

    @Test
    void shouldDeleteTaskAndRemoveFromFile() {
        Task task = new Task(1, "Task 1", "Description 1", Status.NEW, LocalDateTime.of(2000, 12, 3, 4, 5, 6), Duration.ofMinutes(2));
        taskManager.addTask(task);

        taskManager.removeTask(task.getId());

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertNull(loadedManager.getTask(task.getId()), "Удалённая задача не должна быть доступна из файла.");
    }

}


