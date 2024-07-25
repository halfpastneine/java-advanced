package info.kgeorgiy.ja.mikhailov.hello;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Util {

    static int TIME_OUT = 100;
    static int AWAIT_TERMINATION = 100;

    static String decodeString(DatagramPacket requestPacket) {
        return new String(requestPacket.getData(), requestPacket.getOffset(), requestPacket.getLength(), StandardCharsets.UTF_8);
    }

    static String makeRequestMessage(String str, int o1, int o2) {
        return String.format("%s%d_%d", str, o1, o2);
    }

    static String makeResponseMessage(String str) {
        return String.format("Hello, %s", str);
    }

    static byte[] toByte(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    static String toString(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }
}
