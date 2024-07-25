package info.kgeorgiy.ja.mikhailov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer implements HelloServer {

    public static final int MAX_REQUEST_SIZE = 1000;
    private Selector selector;
    private DatagramChannel channel;
    private ExecutorService processor;
    private Thread thread;

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ, new Data(channel.socket().getReceiveBufferSize()));
            processor = Executors.newFixedThreadPool(threads);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    selector.select(this::mainProcess, Util.TIME_OUT);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    break;
                }
            }
        });
        thread.start();
    }

    private void mainProcess(SelectionKey key) {
        Data data = (Data) key.attachment();
        try {
            if (key.isWritable()) {
                if (data.responseQueue.isEmpty()) {
                    key.interestOps(SelectionKey.OP_READ);
                } else {
                    send(key, data);
                }
            } else if (key.isReadable()) {
                receive(key, data);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void send(SelectionKey key, Data data) throws IOException {
        ResponseData responseData = data.responseQueue.remove();
        channel.send(ByteBuffer.wrap(Util.toByte(responseData.message)), responseData.address);
        key.interestOpsOr(SelectionKey.OP_READ);
    }

    private void receive(SelectionKey key, Data data) throws IOException {
        ByteBuffer byteBuffer = data.buffer;
        SocketAddress address = channel.receive(byteBuffer.clear());
        String request = Util.toString(byteBuffer.flip());
        processor.submit(() -> {
            data.responseQueue.add(new ResponseData(Util.makeResponseMessage(request), address));
            key.interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        });
    }

    @Override
    public void close() {
        try {
            if (thread != null) {
                thread.interrupt();
                thread.join();
            }
            if (processor != null) {
                processor.shutdown();
                while (true) {
                    try {
                        if (processor.awaitTermination(Util.AWAIT_TERMINATION, TimeUnit.MILLISECONDS)) {
                            break;
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            if (selector != null) {
                selector.close();
            }
            if (channel != null) {
                channel.close();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    private static class Data {
        final ByteBuffer buffer;
        BlockingQueue<ResponseData> responseQueue = new ArrayBlockingQueue<>(MAX_REQUEST_SIZE);

        public Data(int size) {
            this.buffer = ByteBuffer.allocate(size);
        }
    }

    private static class ResponseData {
        String message;
        SocketAddress address;

        public ResponseData(String message, SocketAddress address) {
            this.message = message;
            this.address = address;
        }
    }

    public static void main(String[] args) {
        AbstractServer.run(new HelloUDPNonblockingServer(), args);
    }
}
