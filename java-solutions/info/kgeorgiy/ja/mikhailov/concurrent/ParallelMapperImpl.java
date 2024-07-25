package info.kgeorgiy.ja.mikhailov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final Thread[] threads;
    private final Deque<Runnable> queue;

    public ParallelMapperImpl(int threadsCount) {
        if (threadsCount < 1) {
            throw new IllegalArgumentException("There must be 1 or more threads");
        }
        threads = new Thread[threadsCount];
        queue = new ArrayDeque<>();
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = makeThreads();
            threads[i].start();
        }
    }

    private Thread makeThreads() {
        return new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    final Runnable runnable;
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            queue.wait();
                        }
                        runnable = queue.poll();
                        queue.notify();
                    }
                    try {
                        runnable.run();
                    } catch (RuntimeException ignored) {
                    }
                }
            } catch (InterruptedException ignored) {
            }
        });
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ArrayList<R> answer = new ArrayList<>(Collections.nCopies(args.size(), null));
        Count count = new Count(args.size());
        int index = 0;
        for (final var a : args) {
            addTask(index++, answer, f, a, count);
        }
        count.waiting();
        if (count.e != null) {
            throw count.e;
        }
        return answer;
    }

    private <R, T> void addTask(final int index, ArrayList<R> answer, Function<? super T, ? extends R> f, T a, Count count) {
        synchronized (queue) {
            queue.push(() -> {
                try {
                    answer.set(index, f.apply(a));
                } catch (RuntimeException e) {
                    count.addException(e);
                }
                count.addCount();
            });
            queue.notify();
        }
    }


    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static class Count {
        private int count;
        private final int size;

        private RuntimeException e = null;

        public Count(int size) {
            count = 0;
            this.size = size;
        }

        public void addCount() {
            synchronized (this) {
                count++;
                if (this.ready()) {
                    this.notify();
                }
            }
        }

        public synchronized void addException(RuntimeException exc) {
            if (e == null) {
                e = exc;
            } else {
                e.addSuppressed(exc);
            }
        }

        public boolean ready() {
            return count == size;
        }

        public void waiting() throws InterruptedException {
            synchronized (this) {
                while (!ready()) {
                    this.wait();
                }
            }
        }
    }
}