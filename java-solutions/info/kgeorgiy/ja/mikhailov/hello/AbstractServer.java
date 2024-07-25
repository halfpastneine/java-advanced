package info.kgeorgiy.ja.mikhailov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Scanner;

public class AbstractServer {
    static void run(HelloServer helloServer, String[] args) {
        if (args == null) {
            System.err.println("No arguments");
            return;
        }
        if (args.length != 2) {
            System.err.println("There must be 2 arguments");
            return;
        }
        for (String s : args) {
            if (s == null) {
                System.err.println("Null args");
                return;
            }
        }
        try {
            helloServer.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            Scanner sc = new Scanner(System.in);
            while(sc.hasNext()) {
                if (sc.next().equals("quit")) {
                    break;
                } else {
                    System.out.println("To stop server write \"quit\"");
                }
            }
            helloServer.close();
        } catch (NumberFormatException e) {
            System.err.println("All args must be integers: " + e.getMessage());
        }
    }
}
