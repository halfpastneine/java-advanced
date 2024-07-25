package info.kgeorgiy.ja.mikhailov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {

    private static final int AWAIT_TERMINATION = 5;

    private ExecutorService executorService;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threads) {
        executorService = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
            for (int i = 0; i < threads; i++) {
                executorService.submit(this::makeResponse);
            }
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    private void makeResponse() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                DatagramPacket response = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                socket.receive(response);
                final byte[] responseBytes = makeRequest(response).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(responseBytes, responseBytes.length, response.getSocketAddress()));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
        socket.close();
        while (true) {
            try {
                if (executorService.awaitTermination(AWAIT_TERMINATION, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private String makeRequest(DatagramPacket requestPacket) {
        return String.format("Hello, %s", new String(requestPacket.getData(), requestPacket.getOffset(), requestPacket.getLength(), StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        AbstractServer.run(new HelloUDPServer(), args);
    }
}
