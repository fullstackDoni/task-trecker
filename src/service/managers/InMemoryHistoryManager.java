package service.managers;
import model.Node;
import model.Task;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    private final Map<Integer, Node> historyMap = new HashMap<>();
    private Node first;
    private Node last;


    @Override
    public void add(Task task) {
        if (task != null) {
            remove(task.getId());
            linkLast(task);
        }
    }

    @Override
    public void remove(int id) {
        Node node = historyMap.remove(id);
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public List<Task> getHistory() {
        List<Task> history = new LinkedList<>();
        Node current = first;
        while (current != null) {
            history.add(current.getTask());
            current = current.getNext();
        }
        return history;
    }

    private void linkLast(Task task) {
        Node newNode = new Node(task, last, null);
        if (last != null) {
            last.setNext(newNode);
        } else {
            first = newNode;
        }
        last = newNode;
        historyMap.put(task.getId(), newNode);
    }

    private void removeNode(Node node) {
        Node prev = node.getPrev();
        Node next = node.getNext();

        if (prev != null) {
            prev.setNext(next);
        } else {
            first = next;
        }

        if (next != null) {
            next.setPrev(prev);
        } else {
            first = prev;
        }
    }
}
