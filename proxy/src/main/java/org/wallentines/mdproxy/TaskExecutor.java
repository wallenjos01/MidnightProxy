package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

@Deprecated
public class TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    private final ThreadPoolExecutor internal;
    private final ClientConnection connection;
    private final Map<String, Queue<Task>> tasks = new HashMap<>();

    public TaskExecutor(int threads, ClientConnection connection) {
        this.internal = new ThreadPoolExecutor(1, threads, 10000L, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        this.connection = connection;
    }

    public void registerTask(String taskQueue, Task task) {
        tasks.computeIfAbsent(taskQueue, k -> new ArrayDeque<>()).add(task);
    }

    public void executeTasks(String taskQueue) {
        executeTasksAsync(taskQueue).join();
    }

    public CompletableFuture<Void> executeTasksAsync(String taskQueue) {

        return CompletableFuture.runAsync(() -> {

            Queue<Task> toExecute = tasks.get(taskQueue);

            if(toExecute == null || toExecute.isEmpty()) {
                return;
            }

            List<Callable<Object>> toCall = new ArrayList<>(toExecute.size());
            while(!toExecute.isEmpty()) {
                Task task = toExecute.remove();
                toCall.add(Executors.callable(() -> {
                    try {
                        task.run(taskQueue, connection);
                    } catch (Throwable th) {
                        LOGGER.error("An error occurred while running a task!", th);
                    }
                }));
            }

            try {
                internal.invokeAll(toCall);
            } catch (InterruptedException ex) {
                LOGGER.error("Unable to complete all tasks!", ex);
            }

        }, internal);
    }

}
