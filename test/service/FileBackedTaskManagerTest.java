package service;

import model.Epic;
import model.SubTask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.file.FileBackedTaskManager;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
        Task task = new Task(7, "taskname", "descr", Status.NEW, LocalDateTime.of(2013, 4, 5, 6, 7, 8), Duration.ofMinutes(4));
        System.out.println(task);
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
    void shouldCalculateEndTimeBasedOnLatestSubtask() {

        LocalDateTime startTime = LocalDateTime.of(2013, 4, 5, 6, 7, 8);
        Duration duration = Duration.ofMinutes(4);
        LocalDateTime endTime = startTime.plus(duration);
        Epic epic1 = new Epic(1, "Epic 1", "Description 1", Status.NEW, startTime, duration,endTime);
        taskManager.addEpic(epic1);
        
        SubTask subTask1 = new SubTask(2, "Subtask 1", "Description 1", Status.DONE, LocalDateTime.of(2024, 12, 1, 10, 0), Duration.ofMinutes(30), epic1.getId());
        SubTask subTask2 = new SubTask(3, "Subtask 2", "Description 2", Status.IN_PROGRESS, LocalDateTime.of(2024, 12, 1, 11, 0), Duration.ofMinutes(45), epic1.getId());
        System.out.println(epic1.getId());
        System.out.println(subTask1);
        System.out.println(subTask2);
        taskManager.addSubtask(subTask1);
        taskManager.addSubtask(subTask2);


        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        Epic loadedEpic = loadedManager.getEpic(epic1.getId());
        assertNotNull(loadedEpic, "Эпик не был восстановлен!");

        assertEquals(2, loadedEpic.getSubTasks().size(), "Количество подзадач не совпадает.");
        assertTrue(loadedEpic.getSubTasks().contains(subTask1.getId()), "Подзадача 1 не восстановлена.");
        assertTrue(loadedEpic.getSubTasks().contains(subTask2.getId()), "Подзадача 2 не восстановлена.");

        loadedManager.updateEpicTimeAndDuration(loadedEpic);

        assertEquals(LocalDateTime.of(2024, 12, 1, 11, 45), loadedEpic.getEndTime(), "Время завершения эпика должно быть основано на самой поздней подзадаче.");
    }

    @Test
    void shouldDeleteTaskAndRemoveFromFile() {
        Task task = new Task(1, "Task 1", "Description 1", Status.NEW, LocalDateTime.of(2000, 12, 3, 4, 5, 6), Duration.ofMinutes(2));
        taskManager.addTask(task);

        taskManager.removeTask(task.getId());

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertNull(loadedManager.getTask(task.getId()), "Удалённая задача не должна быть доступна из файла.");
    }

    @Test
    void shouldRestoreEpicEndTimeCorrectlyAfterLoadingFromFile() {
        LocalDateTime startTime = LocalDateTime.of(2024, 12, 1, 9, 0);
        Duration duration = Duration.ofMinutes(60);
        LocalDateTime endTime = startTime.plus(duration);
        Epic epic = new Epic(1, "Epic 1", "Description 1", Status.NEW, startTime, duration,endTime);
        taskManager.addEpic(epic);
        System.out.println(epic);

        SubTask subTask1 = new SubTask(2, "Subtask 1", "Description 1", Status.DONE, LocalDateTime.of(2024, 12, 1, 10, 0), Duration.ofMinutes(30), epic.getId());
        SubTask subTask2 = new SubTask(3, "Subtask 2", "Description 2", Status.IN_PROGRESS, LocalDateTime.of(2024, 12, 1, 11, 0), Duration.ofMinutes(45), epic.getId());
        taskManager.addSubtask(subTask1);
        System.out.println(subTask1);
        taskManager.addSubtask(subTask2);
        System.out.println(subTask2);


        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        Epic loadedEpic = loadedManager.getEpic(epic.getId());
        assertNotNull(loadedEpic, "Эпик не был восстановлен!");
        assertEquals(LocalDateTime.of(2024, 12, 1, 11, 45), loadedEpic.getEndTime(), "Время завершения эпика не совпадает.");
    }

    @Test
    void shouldRestorePrioritizedTasksSortedCorrectly() {

        Task task1 = new Task(1, "Task 1", "Description 1", Status.NEW, LocalDateTime.of(2024, 12, 1, 9, 0), Duration.ofMinutes(30));
        Task task2 = new Task(2, "Task 2", "Description 2", Status.NEW, LocalDateTime.of(2024, 12, 1, 10, 0), Duration.ofMinutes(45));
        Task task3 = new Task(3, "Task 3", "Description 3", Status.NEW, LocalDateTime.of(2024, 12, 1, 11, 0), Duration.ofMinutes(60));

        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addTask(task3);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> prioritizedTasks = loadedManager.getPriorityTasks();
        assertEquals(3, prioritizedTasks.size(), "Количество задач не совпадает.");
        assertEquals(task1, prioritizedTasks.get(0), "Первая задача должна быть task1.");
        assertEquals(task2, prioritizedTasks.get(1), "Вторая задача должна быть task2.");
        assertEquals(task3, prioritizedTasks.get(2), "Третья задача должна быть task3.");
    }

    @Test
    void shouldRestoreSubtasksCorrectlyAfterLoadingFromFile() {
        LocalDateTime startTime = LocalDateTime.of(2024, 12, 1, 9, 0);
        Duration duration = Duration.ofMinutes(60);
        LocalDateTime endTime = startTime.plus(duration);

        Epic epic = new Epic(9, "Epic 1", "Description 1", Status.NEW, startTime, duration, endTime);
        taskManager.addEpic(epic);
        System.out.println(epic);

        SubTask subTask1 = new SubTask(2, "Subtask 1", "Description 1", Status.DONE,
                LocalDateTime.of(2024, 12, 1, 10, 0), Duration.ofMinutes(30), epic.getId());
        SubTask subTask2 = new SubTask(3, "Subtask 2", "Description 2", Status.IN_PROGRESS,
                LocalDateTime.of(2024, 12, 1, 11, 0), Duration.ofMinutes(45), epic.getId());
        taskManager.addSubtask(subTask1);
        System.out.println(subTask1);
        taskManager.addSubtask(subTask2);
        System.out.println(subTask2);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        Epic loadedEpic = loadedManager.getEpic(epic.getId());
        List<SubTask> loadedSubtasks = loadedEpic.getSubTasks().stream()
                .map(loadedManager::getSubtask)
                .toList();

        assertEquals(2, loadedSubtasks.size(), "Количество подзадач не совпадает.");
        assertTrue(loadedSubtasks.contains(subTask1), "Подзадача 1 не восстановлена.");
        assertTrue(loadedSubtasks.contains(subTask2), "Подзадача 2 не восстановлена.");

        assertEquals(subTask1, loadedSubtasks.get(0), "Первая подзадача должна быть subTask1");
        assertEquals(subTask2, loadedSubtasks.get(1), "Вторая подзадача должна быть subTask2");

        Status expectedEpicStatus = Status.IN_PROGRESS;
        assertEquals(expectedEpicStatus, loadedEpic.getStatus(),
                "Статус эпика должен обновляться в зависимости от статусов подзадач.");
    }

}


