package info.kgeorgiy.ja.mikhailov.walk;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {

    private static final String HASH_ERROR = String.format("%064x", 0);
    private static final byte[] BYTES = new byte[4096];

    private static String bytesToSha256(MessageDigest messageDigest) {
        byte[] shaByte = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : shaByte) {
            String q = Integer.toHexString(0xff & b);
            q = q.length() == 1 ? "0" + q : q;
            sb.append(q);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Args are null");
            return;
        }
        if (args.length != 2) {
            System.err.println("The length of the args must be equal to 2");
            return;
        }
        if (args[0] == null || args[1] == null) {
            System.err.println("Some elements of args array are null");
            return;
        }
        Path fileinput;
        Path fileoutput;
        try {
            fileinput = Path.of(args[0]);
            fileoutput = Path.of(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Path doesn't exist or you've wrote wrong pathname: " + e.getMessage());
            return;
        }
        if (fileoutput.getParent() != null && !Files.exists(fileoutput)) {
            try {
                Files.createDirectories(fileoutput.getParent());
            } catch (IOException e) {
                System.err.println("I/O error or dir exists but is not a directory: " + e.getMessage());
            }
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(fileinput)) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(fileoutput)) {
                String filename;
                while ((filename = bufferedReader.readLine()) != null) {
                    try {
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                        calcHash(bufferedWriter, filename, messageDigest);
                    } catch (NoSuchAlgorithmException e) {
                        System.err.println("Algorithm is not available: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("There is a problem with your output file: " + e.getMessage());
            }
        } catch (AccessDeniedException e) {
            System.err.println("Access denied file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("There is a problem with your input file: " + e.getMessage());
        }
    }

    private static void calcHash(BufferedWriter bufferedWriter, String filename, MessageDigest messageDigest) throws IOException {
        try {
            Path fname = Paths.get(filename);
            try (InputStream reader = Files.newInputStream(fname)) {
                int read;
                while ((read = reader.read(BYTES)) != -1) {
                    messageDigest.update(BYTES, 0, read);
                }
                bufferedWriter.write(bytesToSha256(messageDigest) + " " + filename + System.lineSeparator());
            } catch (IOException e) {
                System.err.println("There is a problem with your input file: " + e.getMessage());
                bufferedWriter.write(HASH_ERROR + " " + filename + System.lineSeparator());
            }
        } catch (InvalidPathException e) {
            System.err.println("Path doesn't exist or you've wrote wrong pathname: " + e.getMessage());
            bufferedWriter.write(HASH_ERROR + " " + filename + System.lineSeparator());
        }
    }
}