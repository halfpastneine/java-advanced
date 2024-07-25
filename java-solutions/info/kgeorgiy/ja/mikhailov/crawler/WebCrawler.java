package info.kgeorgiy.ja.mikhailov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    final private Downloader downloader;
    final private ExecutorService load;
    final private ExecutorService extract;

    public WebCrawler(final Downloader downloader, final int downloads, final int extractors, final int perHost) {
        this.downloader = downloader;
        load = Executors.newFixedThreadPool(downloads);
        extract = Executors.newFixedThreadPool(extractors);
    }


    @Override
    public Result download(String url, int depth) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> exception = new ConcurrentHashMap<>();
        final Set<String> used = ConcurrentHashMap.newKeySet();
        Queue<String> queue = new ConcurrentLinkedDeque<>();
        queue.add(url);
        for (int i = depth; i >= 1; i--) {
            Phaser phaser = new Phaser(1);
            while (!queue.isEmpty()) {
                phaser.register();
                final String link = queue.poll();
                load.submit(() -> downloadTasks(downloaded, exception, used, link, depth, phaser));
            }
            phaser.arriveAndAwaitAdvance();
            queue.addAll(used);
            used.clear();
        }
        return new Result(new ArrayList<>(downloaded), exception);
    }

    private void downloadTasks(Set<String> downloaded, Map<String, IOException> exception, Set<String> used, String url, int depth, Phaser phaser) {
        if (!exception.containsKey(url) && downloaded.add(url)) {
            try {
                Document document = downloader.download(url);
                if (depth > 1) {
                    extract.submit(() -> {
                        try {
                            used.addAll(document.extractLinks());
                        } catch (IOException ignored) {
                        } finally {
                            phaser.arrive();
                        }
                    });
                } else {
                    phaser.arrive();
                }
            } catch (IOException e) {
                downloaded.remove(url);
                exception.put(url, e);
                phaser.arrive();
            }
        } else {
            phaser.arrive();
        }
    }

    @Override
    public void close() {
        load.shutdown();
        extract.shutdown();
        while (true) {
            try {
                if (load.awaitTermination(5, TimeUnit.SECONDS) && extract.awaitTermination(5, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {}
        }
    }

    public static void main(String[] args) {
        if (args == null || args[0] == null || args.length > 5) {
            System.err.println("Args must be: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        int[] arguments = new int[5];
        for (int i = 1; i < arguments.length; i++) {
            if (args[i] == null) {
                arguments[i] = 1;
            } else {
                try {
                    arguments[i] = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.printf("On position %d must be integer value", i);
                    return;
                }
            }
        }
        try {
            final Crawler crawler = new WebCrawler(new CachingDownloader(1), arguments[2], arguments[3], arguments[4]);
            final Result result = crawler.download(args[0], arguments[1]);
            for (final String download : result.getDownloaded()) {
                System.out.println(download);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
