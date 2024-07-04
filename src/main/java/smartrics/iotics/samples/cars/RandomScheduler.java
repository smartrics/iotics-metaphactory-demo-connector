package smartrics.iotics.samples.cars;

import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class RandomScheduler<T> {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService taskExecutor;
    private final Random random = new Random();

    private final int period;
    private final int variance;
    private volatile boolean running = true;

    public RandomScheduler(int period, int variance, int tasksThreadPoolSize) {
        this.period = period;
        this.variance = variance;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.taskExecutor = Executors.newFixedThreadPool(tasksThreadPoolSize);
    }

    public void start(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        scheduleNextTask(task, onSuccess, onError);
    }

    public void stop() {
        running = false;
        taskExecutor.shutdown();
    }

    private void scheduleNextTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        if (!running) {
            return;
        }

        int randomDelay = period + random.nextInt(2 * variance + 1) - variance;
        scheduler.schedule(() -> runTask(task, onSuccess, onError), randomDelay, TimeUnit.SECONDS);
    }

    private void runTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        taskExecutor.submit(() -> {
            try {
                T result = task.call();

                onSuccess.accept(result);
            } catch (Exception e) {
                onError.accept(e);
            } finally {
                scheduleNextTask(task, onSuccess, onError);
            }
        });
    }

}
