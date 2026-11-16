package ui;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Runs blocking work off the EDT; delivers results and restores controls on the EDT. */
public final class BackgroundTask<T> extends SwingWorker<T, Void> {
    private final Callable<T> work;
    private final Consumer<T> success;
    private final Consumer<Throwable> failure;
    private final Runnable finished;

    public BackgroundTask(Callable<T> work, Consumer<T> success,
                          Consumer<Throwable> failure, Runnable finished) {
        this.work = work;
        this.success = success;
        this.failure = failure;
        this.finished = finished;
    }

    @Override
    protected T doInBackground() throws Exception {
        return work.call();
    }

    @Override
    protected void done() {
        try {
            success.accept(get());
        } catch (ExecutionException ex) {
            failure.accept(ex.getCause());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failure.accept(ex);
        } catch (CancellationException ex) {
            failure.accept(ex);
        } finally {
            finished.run();
        }
    }
}
