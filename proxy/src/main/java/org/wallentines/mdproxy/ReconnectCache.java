package org.wallentines.mdproxy;

import java.util.Map;
import java.util.concurrent.*;

public class ReconnectCache {

    private final Executor executor;
    private final int timeout;
    private final Map<String, ClientConnectionImpl> awaiting = new ConcurrentHashMap<>();

    public ReconnectCache(int maxThreads, int reconnectTimeout) {
        this.executor = new ThreadPoolExecutor(1, maxThreads, reconnectTimeout * 2L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(maxThreads));
        this.timeout = reconnectTimeout;
    }

    public void set(String id, ClientConnectionImpl impl) {
        awaiting.put(id, impl);
        executor.execute(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ex) {
                // Ignore
            }
            awaiting.remove(id);
        });
    }

    public void clear(String id) {
        awaiting.remove(id);
    }

    public ClientConnectionImpl get(String id) {
        return awaiting.get(id);
    }


}