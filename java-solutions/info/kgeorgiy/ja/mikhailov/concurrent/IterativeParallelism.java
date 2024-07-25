package info.kgeorgiy.ja.mikhailov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {

    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this(null);
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return threadsImpl(threads,
                values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return threadsImpl(threads,
                values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(a -> a));
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return threadsImpl(threads,
                values,
                stream -> stream.filter(predicate).toList().size(),
                integerStream -> integerStream.mapToInt(i -> i).sum());
    }

    private <T, R> R threadsImpl(int threads, List<? extends T> values, Function<Stream<? extends T>, R> findFunc, Function<Stream<R>, R> answerFunc) throws InterruptedException {
        checkValid(threads);
        threads = Math.min(threads, values.size());
        ArrayList<Stream<? extends T>> streams = makeStreams(threads, values);
        ArrayList<R> answer = new ArrayList<>(Collections.nCopies(threads, null));
        if (parallelMapper != null) {
           return answerFunc.apply(parallelMapper.map(findFunc, streams).stream());
        }
        makeMultiThreads(threads, findFunc, answer, streams);
        return answerFunc.apply(answer.stream());
    }

    private <T> ArrayList<Stream<? extends T>> makeStreams(int threads, List<? extends T> values) {
        final int listPartition = values.size() / threads;
        int rest = values.size() % threads;
        ArrayList<Stream<? extends T>> streams = new ArrayList<>();
        int l = 0;
        int r = listPartition;
        for (int i = 0; i < threads; i++) {
            streams.add(values.subList(l, r).stream());
            l = r;
            r = l + listPartition + (rest-- > 0 ? 1 : 0);
        }
        return streams;
    }

    private <T, R> void makeMultiThreads(int threads, Function<Stream<? extends T>, R> findFunc, ArrayList<R> answer, ArrayList<Stream<? extends T>> streams) throws InterruptedException {
        ArrayList<Thread> multiThreads = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            Thread thread = new Thread(() -> answer.set(index, findFunc.apply(streams.get(index))));
            thread.start();
            multiThreads.add(thread);
        }
        for (Thread thread : multiThreads) {
            thread.join();
        }
    }

    private <T> void checkValid(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("There are must be 1 or more threads");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        IterativeParallelism it = new IterativeParallelism();
        it.any(1, null, null);
    }
}

