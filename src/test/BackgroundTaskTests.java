package test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import ui.BackgroundTask;

/** Verifies thread separation, EDT responsiveness, error delivery and cleanup. */
public final class BackgroundTaskTests {
    public static void main(String[] args) throws Exception {
        verify(false);
        verify(true);
        System.out.println("2 background-task scenarios passed");
    }

    private static void verify(boolean fail) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch pulse = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        RuntimeException expected = new RuntimeException("simulated database failure");
        SwingUtilities.invokeAndWait(() -> new BackgroundTask<String>(() -> {
            if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("work ran on EDT");
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("worker timed out");
            if (fail) throw expected;
            return "imported";
        }, value -> {
            if (!SwingUtilities.isEventDispatchThread() || fail || !"imported".equals(value))
                error.set(new AssertionError("wrong success callback"));
        }, cause -> {
            if (!SwingUtilities.isEventDispatchThread() || !fail || cause != expected)
                error.set(new AssertionError("wrong failure callback", cause));
        }, () -> {
            if (!SwingUtilities.isEventDispatchThread()) error.set(new AssertionError("cleanup off EDT"));
            finished.countDown();
        }).execute());
        try {
            if (!started.await(5, TimeUnit.SECONDS)) throw new AssertionError("worker did not start");
            SwingUtilities.invokeLater(pulse::countDown);
            if (!pulse.await(5, TimeUnit.SECONDS)) throw new AssertionError("EDT blocked during work");
        } finally {
            release.countDown();
        }
        if (!finished.await(5, TimeUnit.SECONDS)) throw new AssertionError("cleanup did not run");
        if (error.get() != null) throw new AssertionError("callback failed", error.get());
    }
}
