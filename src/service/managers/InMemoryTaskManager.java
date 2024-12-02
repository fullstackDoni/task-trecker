package service.managers;
import exception.ValidationException;
import model.Epic;
import model.enums.Status;
import model.SubTask;
import model.Task;
import service.Managers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {

    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, SubTask> subtasks = new HashMap<>();
    protected int id = 0;
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    private final Comparator<Task> taskComparator
            = Comparator.comparing(Task::getStartTime,
            Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Task::getId);
    protected final Set<Task> prioritizedTasks = new TreeSet<>(taskComparator);

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<SubTask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public Task getTask(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public SubTask getSubtask(int id) {
        SubTask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public void addTask(Task task) {
        if (hasOverlaps(task)) {
            throw new ValidationException("Task overlaps with an existing task.");
        }
        task.setId(++id);
        tasks.put(task.getId(), task);
        prioritizedTasks.add(task);
        historyManager.add(task);
    }

    @Override
    public void addEpic(Epic epic) {
        epic.setId(++id);
        epics.put(epic.getId(), epic);
    }

    @Override
    public void addSubtask(SubTask subtask) {
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            if (hasOverlaps(subtask)) {
                throw new ValidationException("Subtask overlaps with an existing task.");
            }
            subtask.setId(++id);
            subtasks.put(subtask.getId(), subtask);
            prioritizedTasks.add(subtask);
            epic.addSubTask(subtask.getId());
            updateEpicTimeAndDuration(epic);
            updateStatus(epic);
        } else {
            throw new ValidationException("Epic with ID " + subtask.getEpicId() + " not found.");
        }
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            if (hasOverlaps(task)) {
                throw new ValidationException("Task overlaps with an existing task.");
            }
            prioritizedTasks.remove(tasks.get(task.getId()));
            tasks.put(task.getId(), task);
            prioritizedTasks.add(task);
        } else {
            throw new ValidationException("Task with ID " + task.getId() + " not found.");
        }
    }


    @Override
    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            Epic savedEpic = epics.get(epic.getId());
            savedEpic.setName(epic.getName());
            savedEpic.setDescription(epic.getDescription());
            updateEpicTimeAndDuration(savedEpic);
        } else {
            throw new ValidationException("Epic with ID " + epic.getId() + " not found.");
        }
    }


    @Override
    public void updateSubtask(SubTask subtask) {
        System.out.println("Subtask ID: " + subtask.getId());
        if (subtasks.containsKey(subtask.getId())) {
            if (hasOverlaps(subtask)) {
                throw new ValidationException("Subtask overlaps with an existing task.");
            }
            prioritizedTasks.remove(subtasks.get(subtask.getId()));
            subtasks.put(subtask.getId(), subtask);
            prioritizedTasks.add(subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicTimeAndDuration(epic);
                updateStatus(epic);
            }
        } else {
            throw new ValidationException("Subtask with ID " + subtask.getId() + " not found.");
        }
    }

    private void updateStatus(Epic epic) {
        List<SubTask> subTasks = getSubtasksOfEpic(epic.getId());

        if (subTasks.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean hasInProgress = false;
        boolean hasNotDone = false;

        for (SubTask subTask : subTasks) {
            if (subTask.getStatus() == Status.IN_PROGRESS) {
                hasInProgress = true;
            } else if (subTask.getStatus() != Status.DONE) {
                hasNotDone = true;
            }
        }

        if (hasInProgress) {
            epic.setStatus(Status.IN_PROGRESS);
        } else if (!hasNotDone) {
            epic.setStatus(Status.DONE);
        } else {
            epic.setStatus(Status.NEW);
        }
    }

    @Override
    public void removeTask(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            historyManager.remove(id);
        }
    }

    @Override
    public void removeEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (Integer subtaskId : epic.getSubTasks()) {
                SubTask subTask = subtasks.remove(subtaskId);
                if (subTask != null) {
                    prioritizedTasks.remove(subTask);
                    historyManager.remove(subtaskId);
                }
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void removeSubtask(int id) {
        SubTask subtask = subtasks.remove(id);
        if (subtask != null) {
            prioritizedTasks.remove(subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubTask(id);
                updateEpicTimeAndDuration(epic);
                updateStatus(epic);
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllTasks() {
        for (Integer taskId : tasks.keySet()) {
            historyManager.remove(taskId);
        }
        tasks.clear();
        prioritizedTasks.removeIf(task -> task instanceof Task);
    }

    @Override
    public void deleteAllEpics() {
        for (Integer epicId : epics.keySet()) {
            Epic epic = epics.get(epicId);
            if (epic != null) {
               for (Integer subtaskId : epic.getSubTasks()) {
                   SubTask subTask = subtasks.remove(subtaskId);
                   if (subTask != null) {
                       prioritizedTasks.remove(subTask);
                       historyManager.remove(subtaskId);
                   }
               }
            }
            historyManager.remove(epicId);
        }
        epics.clear();
        subtasks.clear();
    }

    @Override
    public void deleteAllSubtasks() {
        for (SubTask subtask : subtasks.values()) {
            prioritizedTasks.remove(subtask);
            historyManager.remove(subtask.getId());
        }
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.clearSubTasks();
            updateEpicTimeAndDuration(epic);
            updateStatus(epic);
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<SubTask> getSubtasksOfEpic(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic != null) {
            List<SubTask> result = new ArrayList<>();
            for (Integer subTaskId : epic.getSubTasks()) {
                SubTask subTask = subtasks.get(subTaskId);
                if (subTask != null) {
                    result.add(subTask);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public List<Task> getPriorityTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    private boolean hasOverlaps(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getDuration() == null) {
            return false;
        }

        LocalDateTime startTime = newTask.getStartTime();
        LocalDateTime endTime = startTime.plus(newTask.getDuration());

        return prioritizedTasks.stream()
                .filter(existingTask -> existingTask.getId() != newTask.getId())
                .anyMatch(existingTask -> {
                    if (existingTask.getStartTime() == null || existingTask.getDuration() == null) {
                        return false;
                    }

                    LocalDateTime existingStart = existingTask.getStartTime();
                    LocalDateTime existingEnd = existingStart.plus(existingTask.getDuration());
                    return startTime.isBefore(existingEnd) && endTime.isAfter(existingStart);
        });
    }

    private void updateEpicTimeAndDuration(Epic epic) {
        LocalDateTime earliestStart = null;
        Duration totalDuration = Duration.ZERO;

        for (Integer subTaskId : epic.getSubTasks()) {
            SubTask subTask = subtasks.get(subTaskId);

            if (subTask != null) {
                if (earliestStart == null || subTask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subTask.getStartTime();
                }
                totalDuration = totalDuration.plus(subTask.getDuration());
            }
        }

        epic.setStartTime(earliestStart);
        epic.setDuration(totalDuration);
    }

}
