package info.kgeorgiy.ja.mikhailov.hello;


import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class HelloUDPNonblockingClient implements HelloClient {

    private static class Data {
        public int count = 1;
        public final int threadNumber;
        public final ByteBuffer buffer;

        public Data(int threadNumber, int size) {
            this.threadNumber = threadNumber;
            this.buffer = ByteBuffer.allocate(size);
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final Selector selector;
        final SocketAddress address;
        try {
            selector = Selector.open();
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        addChannel(threads, selector, address);
        process(prefix, requests, selector, address);
        close(selector);
    }

    private static void addChannel(int threads, Selector selector, SocketAddress address) {
        for (int threadNumber = 1; threadNumber <= threads; threadNumber++) {
            try {
                final DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(address);
                channel.register(selector, SelectionKey.OP_WRITE,
                        new Data(threadNumber, channel.socket().getReceiveBufferSize()));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void process(String prefix, int requests, Selector selector, SocketAddress address) {
        while (!Thread.currentThread().isInterrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(Util.TIME_OUT);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            if (selectedKeys.isEmpty()) {
                selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
            } else {
                selectedKeys.forEach(key -> {
                    DatagramChannel channel = (DatagramChannel) key.channel();
                    Data data = (Data) key.attachment();
                    if (key.isWritable()) {
                        sendMessage(key, address, prefix, data, channel);
                    } else if (key.isReadable()) {
                        receiveMessage(key, prefix, requests, data, channel);
                    }
                });
                selectedKeys.clear();
            }
        }
    }

    private void receiveMessage(SelectionKey key, String prefix, int requests, Data data, DatagramChannel channel) {
        String requestMessage = Util.makeRequestMessage(prefix, data.threadNumber, data.count);
        data.buffer.clear();
        try {
            channel.receive(data.buffer.clear());
            data.buffer.flip();
            if (Util.toString(data.buffer).contains(requestMessage)) {
                System.out.println(requestMessage);
                data.count++;
            }
            if (data.count >= requests + 1) {
                channel.close();
            } else {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void sendMessage(SelectionKey key, SocketAddress address, String prefix, Data data, DatagramChannel channel) {
        String requestMessage = Util.makeRequestMessage(prefix, data.threadNumber, data.count);
        data.buffer.clear().put(Util.toByte(requestMessage)).flip();
        try {
            channel.send(data.buffer, address);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void close(Selector selector) {
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        AbstractClient.run(new HelloUDPNonblockingClient(), args);
    }
}
