package service;

import exception.ValidationException;
import model.Epic;
import model.SubTask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.managers.TaskManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {
    T taskManager;

    protected abstract T createTaskManager();


    @BeforeEach
    void setUp(){
        taskManager = createTaskManager();
    }

    @Test
    void shouldAddAndRetrieveTask() {
        Task task = new Task(1,"Task 1", "Description 1", Status.NEW,LocalDateTime.of(2000,12,3,4,5,6), Duration.ofMinutes(2));
        taskManager.addTask(task);

        Task retrievedTask = taskManager.getTask(task.getId());
        Assertions.assertEquals(task, retrievedTask, "Добавленная задача должна быть доступна по ID.");
    }

    @Test
    void shouldAddAndRetrieveEpic() {
        Epic epic = new Epic(1,"Epic 1", "Description 1",Status.NEW,Duration.ofMinutes(1));
        taskManager.addEpic(epic);

        Epic retrievedEpic = taskManager.getEpic(epic.getId());
        assertEquals("Добавленный эпик должен быть доступен по ID.", epic, retrievedEpic);
    }

    @Test
    void shouldAddAndRetrieveSubtask() {
        Epic epic = new Epic(4,"Epic 1", "Description 1",Status.NEW,Duration.ofMinutes(1));
        taskManager.addEpic(epic);

        SubTask subtask = new SubTask(5,"SubTask 1", "Description 1", Status.NEW,LocalDateTime.of(2016,4,5,6,7,8), Duration.ofMinutes(1), epic.getId());
        taskManager.addSubtask(subtask);

        SubTask retrievedSubtask = taskManager.getSubtask(subtask.getId());
        assertEquals("Добавленная подзадача должна быть доступна по ID.", subtask, retrievedSubtask);

        List<SubTask> subtasks = taskManager.getSubtasksOfEpic(epic.getId());
        assertTrue(subtasks.contains(subtask), "Подзадача должна быть связана с эпиком.");
    }

    @Test
    void shouldUpdateTask() {
        Task task = new Task(7,"Task 1", "Description 1", Status.NEW, LocalDateTime.of(2013, 3, 4, 5, 6, 7), Duration.ofMinutes(2));
        taskManager.addTask(task);

        task.setName("Updated Task");
        taskManager.updateTask(task);

        Task updatedTask = taskManager.getTask(task.getId());

        Assertions.assertEquals("Updated Task", updatedTask.getName(), "Задача должна обновляться.");
    }

    @Test
    void shouldDeleteTask() {
        Task task = new Task(3,"Task 1", "Description 1",Status.NEW, LocalDateTime.of(2004,4,4,4,4,4), Duration.ofMinutes(2));
        taskManager.addTask(task);

        taskManager.removeTask(task.getId());
        assertNull(taskManager.getTask(task.getId()), "Удалённая задача не должна быть доступна.");
    }

    @Test
    void shouldDeleteEpicAndSubtasks() {
        Epic epic = new Epic(3,"Epic 1", "Description 1",Status.NEW,Duration.ofMinutes(1));
        taskManager.addEpic(epic);

        SubTask subtask = new SubTask(4,"SubTask 1", "Description 1",Status.NEW, LocalDateTime.of(2004,3,3,3,3,3), Duration.ofMinutes(1), epic.getId());
        taskManager.addSubtask(subtask);

        taskManager.removeEpic(epic.getId());
        assertNull(taskManager.getEpic(epic.getId()), "Удалённый эпик не должен быть доступен.");
        assertNull(taskManager.getSubtask(subtask.getId()), "Подзадачи удалённого эпика не должны быть доступны.");
    }

    @Test
    void shouldRetrieveAllSubtasks() {
        Epic epic = new Epic(1,"Epic 1", "Description 1", Status.NEW, Duration.ofMinutes(1));
        taskManager.addEpic(epic);

        SubTask subtask1 = new SubTask(2,"SubTask 1", "Description 1", Status.NEW, LocalDateTime.of(1990, 12, 4, 5, 6, 7), Duration.ofMinutes(1), epic.getId());
        SubTask subtask2 = new SubTask(3,"SubTask 2", "Description 2", Status.NEW, LocalDateTime.of(2001, 5, 6, 7, 8, 9), Duration.ofMinutes(2), epic.getId());
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);

        List<SubTask> subtasks = taskManager.getAllSubtasks();

        Assertions.assertEquals(2, subtasks.size(), "Должно быть добавлено две подзадачи.");

        assertTrue(subtasks.contains(subtask1));
        assertTrue(subtasks.contains(subtask2));
    }

    @Test
    void shouldCheckForOverlaps() {
        Task task1 = new Task(4, "Task 1", "Description 1", Status.NEW,
                LocalDateTime.of(2018, 6, 7, 8, 9, 9), Duration.ofMinutes(2));
        taskManager.addTask(task1);

        Task overlappingTask = new Task(5, "Overlapping Task", "Description", Status.NEW,
                LocalDateTime.of(2018, 6, 7, 8, 9, 8), Duration.ofMinutes(2));

        assertThrows(ValidationException.class,
                () -> taskManager.addTask(overlappingTask),
                "Пересекающиеся задачи не должны быть добавлены.");
    }

    @Test
    void shouldDeleteTaskAndHistory() {
        Task task = new Task(3,"Task 1", "Description 1", Status.NEW, LocalDateTime.of(2004,4,4,4,4,4), Duration.ofMinutes(2));
        taskManager.addTask(task);

        taskManager.removeTask(task.getId());

        List<Task> history = taskManager.getHistory();

        assertFalse(history.contains(task), "Задача не должна быть в истории после удаления.");
    }

    @Test
    void shouldRemoveEpicAndSubtasksFromHistory() {
        Epic epic = new Epic(3,"Epic 1", "Description 1", Status.NEW, Duration.ofMinutes(1));
        taskManager.addEpic(epic);

        SubTask subtask = new SubTask(4,"SubTask 1", "Description 1", Status.NEW, LocalDateTime.of(2004,3,3,3,3,3), Duration.ofMinutes(1), epic.getId());
        taskManager.addSubtask(subtask);

        taskManager.removeEpic(epic.getId());

        List<Task> history = taskManager.getHistory();

        assertFalse(history.contains(epic), "Эпик не должен быть в истории после удаления.");
        assertFalse(history.contains(subtask), "Подзадача не должна быть в истории после удаления эпика.");
    }
}
