package info.kgeorgiy.ja.mikhailov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

public class AbstractClient {

    static void run(HelloClient helloClient, String[] args) {
        if (args == null) {
            System.err.println("No arguments");
            return;
        }
        if (args.length != 5) {
            System.err.println("There are must be 5 arguments");
            return;
        }
        for (String str : args) {
            if (str == null) {
                System.err.println("Null args");
                return;
            }
        }
        try {
            helloClient.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("On arguments position 2, 4 and 5 must be integers: " + e.getMessage());
        }
    }

}
