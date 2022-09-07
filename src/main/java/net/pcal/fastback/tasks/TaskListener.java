package net.pcal.fastback.tasks;

public interface TaskListener {

    void feedback(String message);

    void error(String message);
}
