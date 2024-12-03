package model;
import model.enums.Status;
import java.time.Duration;
import java.time.LocalDateTime;

public class SubTask extends Task {

    private final int epicId;

    public SubTask(int id, String name, String description, Status status,LocalDateTime startTime,Duration duration, int epicId) {
        super(id, name, description, status, startTime, duration);
        this.epicId = epicId;
    }

    public SubTask(int id,String name, String description,LocalDateTime startTime,Duration duration, int epicId) {
        super(id, name, description, startTime, duration);
        this.epicId = epicId;
    }


    public int getEpicId() {
        return epicId;
    }

    @Override
    public String toString() {
        return "SubTask{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", epicId=" + epicId +
                ", startTime=" + startTime +
                ", duration=" + duration +
                '}';
    }
}