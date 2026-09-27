package com.kubling.jdbc.tracing;

import io.opentelemetry.context.Context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} wrapper that automatically propagates the current
 * OpenTelemetry {@link Context} to asynchronous tasks.
 * <p>
 * This class is useful when you submit tasks to an executor from within a traced
 * operation, and you want the resulting spans to preserve the same trace and baggage
 * context across threads.
 * <p>
 * Example usage:
 * <pre>{@code
 * ExecutorService delegate = Executors.newFixedThreadPool(4);
 * ExecutorService tracedExecutor = new ContextPropagatingExecutorService(delegate);
 *
 * // Inside a traced method
 * Span span = tracer.spanBuilder("async-operation").startSpan();
 * try (Scope scope = span.makeCurrent()) {
 *     tracedExecutor.submit(() -> {
 *         // The same trace context is automatically active here
 *         Span child = tracer.spanBuilder("child-task").startSpan();
 *         child.end();
 *     });
 * } finally {
 *     span.end();
 * }
 * }</pre>
 *
 * <p>All methods that accept {@link Runnable} or {@link Callable} ensure that
 * the task executes under the {@link Context} that was current at submission time.
 * Collection-returning methods like {@link #invokeAll(Collection)} and
 * {@link #invokeAny(Collection)} preserve this same behavior.
 *
 * <p>This wrapper does <strong>not</strong> modify the underlying task scheduling
 * or threading behavior. It only decorates submitted tasks to ensure that
 * {@link Context#current()} at submission time is restored during execution.
 * </ul>
 */
public class ContextPropagatingExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public ContextPropagatingExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        Context parent = Context.current();
        delegate.execute(() -> {
            try (final var ignored = parent.makeCurrent()) {
                command.run();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Context parent = Context.current();
        return delegate.submit(() -> {
            try (final var ignored = parent.makeCurrent()) {
                return task.call();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Context parent = Context.current();
        return delegate.submit(() -> {
            try (final var ignored = parent.makeCurrent()) {
                task.run();
            }
        }, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        Context parent = Context.current();
        return delegate.submit(() -> {
            try (final var ignored = parent.makeCurrent()) {
                task.run();
            }
        });
    }

    // delegate all other methods
    @Override public void shutdown() { delegate.shutdown(); }
    @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
    @Override public boolean isShutdown() { return delegate.isShutdown(); }
    @Override public boolean isTerminated() { return delegate.isTerminated(); }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return delegate.awaitTermination(timeout, unit); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException { return delegate.invokeAll(tasks); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException { return delegate.invokeAll(tasks, timeout, unit); }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException { return delegate.invokeAny(tasks); }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return delegate.invokeAny(tasks, timeout, unit); }
}
