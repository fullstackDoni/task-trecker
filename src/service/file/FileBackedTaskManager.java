package service.file;
import exception.ManagerSaveException;
import model.Epic;
import model.SubTask;
import model.Task;
import model.enums.Status;
import model.enums.TaskType;
import service.managers.InMemoryTaskManager;
import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {

    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    @Override
    public void addTask(Task task) {
        super.addTask(task);
        save();
    }

    @Override
    public void addEpic(Epic epic) {
        super.addEpic(epic);
        save();
    }

    @Override
    public void addSubtask(SubTask subtask) {
        super.addSubtask(subtask);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(SubTask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void removeTask(int id) {
        super.removeTask(id);
        save();
    }

    @Override
    public void removeEpic(int epicId) {
        super.removeEpic(epicId);
        save();
    }

    @Override
    public void removeSubtask(int subtaskId) {
        super.removeSubtask(subtaskId);
        save();
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
        save();
    }

    @Override
    public void deleteAllEpics() {
        epics.clear();
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        subtasks.clear();
        save();
    }

    private void save() {
        try (Writer writer = new FileWriter(file)) {
            writer.write("id,type,name,description,status,startTime,duration,endTime,epicId\n");
            for (Task task : getAllTasks()) {
                writer.write(toCSV(task) + "\n");
            }
            for (Epic epic : getAllEpics()) {
                writer.write(toCSV(epic) + "\n");
            }
            for (SubTask subtask : getAllSubtasks()) {
                writer.write(toCSV(subtask) + "\n");
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении в файл", e);
        }
    }

    private String toCSV(Task task) {
        String type = task instanceof Epic ? "EPIC" : (task instanceof SubTask ? "SUBTASK" : "TASK");
        String epicId = task instanceof SubTask ? String.valueOf(((SubTask) task).getEpicId()) : "";

        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                task.getId(),
                type,
                task.getName(),
                task.getDescription(),
                task.getStatus(),
                task.getStartTime() != null ? task.getStartTime().toString() : "",
                task.getDuration() != null ? task.getDuration().toString() : "",
                task.getEndTime() != null ? task.getEndTime().toString() : "",
                epicId
        );
    }

    private Task fromCSV(String[] fields) {
        try {
            int id = Integer.parseInt(fields[0]);
            TaskType type = TaskType.valueOf(fields[1].toUpperCase());
            String name = fields[2];
            String description = fields[3];
            Status status = Status.valueOf(fields[4].toUpperCase());
            LocalDateTime startTime = fields[5].isEmpty() ? null : LocalDateTime.parse(fields[5]);
            Duration duration = fields[6].isEmpty() ? Duration.ZERO : Duration.parse(fields[6]);
            LocalDateTime endTime = fields[7].isEmpty() ? null : LocalDateTime.parse(fields[7]);

            switch (type) {
                case TASK:
                    return new Task(id, name, description, status, startTime, duration);
                case EPIC:
                    return new Epic(id, name, description, status, startTime, duration,endTime);
                case SUBTASK:
                    int epicId = Integer.parseInt(fields[8]);
                    return new SubTask(id, name, description, status, startTime, duration, epicId);
                default:
                    throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
            }
        } catch (Exception e) {
            throw new ManagerSaveException("Ошибка при разборе строки CSV: " + String.join(",", fields), e);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        int maxId = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) {
                return manager;
            }
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) continue;

                String[] fields = line.split(",");
                Task task = manager.fromCSV(fields);

                maxId = Math.max(maxId, task.getId());

                if (task instanceof Epic epic) {
                    manager.epics.put(task.getId(), epic);
                    manager.prioritizedTasks.add(task);
                } else if (task instanceof SubTask subtask) {
                    manager.subtasks.put(task.getId(), subtask);
                    Epic parentEpic = manager.epics.get(subtask.getEpicId());
                    if (parentEpic != null) {
                        parentEpic.addSubTask(task.getId());
                    }
                    manager.prioritizedTasks.add(subtask);
                } else {
                    manager.tasks.put(task.getId(), task);
                    manager.prioritizedTasks.add(task);
                }
            }
            manager.id = maxId + 1;
            return manager;
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при загрузке из файла: " + file.getName(), e);
        }
    }


}
