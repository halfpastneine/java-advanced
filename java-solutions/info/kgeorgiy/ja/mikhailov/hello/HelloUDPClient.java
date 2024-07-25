package info.kgeorgiy.ja.mikhailov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    private static final int TIME_WAIT = 100;
    private static final int AWAIT_TERMINATION = 5;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final SocketAddress socketAddress = new InetSocketAddress(host, port);
        IntStream.range(0, threads).forEach(i -> {
            executorService.submit(() -> makeRequests(socketAddress, prefix, i, requests));
        });
        close(executorService);
    }

    private void makeRequests(SocketAddress socketAddress, String prefix, int threadNumber, int requests) {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(TIME_WAIT);
            for (int requestNumber = 0; requestNumber < requests; requestNumber++) {
                final String requestMessage = Util.makeRequestMessage(prefix, threadNumber + 1, requestNumber + 1);
                String responseMessage = null;
                // :NOTE: Local Response
                // contains
                while (!Objects.equals(String.format("Hello, %s", requestMessage), responseMessage)) {
                    try {
                        // :NOTE: Util class with methods to convert String <-> DatagramPacket
                        DatagramPacket requestPacket = new DatagramPacket(new byte[0], 0, socketAddress);
                        requestPacket.setData(requestMessage.getBytes(StandardCharsets.UTF_8));
                        datagramSocket.send(requestPacket);
                        requestPacket = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()], datagramSocket.getReceiveBufferSize());
                        datagramSocket.receive(requestPacket);
                        responseMessage = Util.decodeString(requestPacket);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
                System.out.println(responseMessage);
            }
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    private void close(ExecutorService executorService) {
        executorService.shutdown();
        while (true) {
            try {
                if (executorService.awaitTermination(AWAIT_TERMINATION, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
            }
        }
    }


    public static void main(String[] args) {
        AbstractClient.run(new HelloUDPClient(), args);
    }
}
