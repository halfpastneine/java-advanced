package info.kgeorgiy.ja.mikhailov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * An implementation of the {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {

    /**
     *This char is used as a front slash.
     */
    private final static char FRONT_SLASH = '/';
    /**
     *This char is used as a dot.
     */
    private final static char DOT = '.';
    /**
     *This char is used as front a semicolon.
     */
    private final static char SEMI_COLON = ';';


    /**
     * {@link #compile(Class, Path)}
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException aaaaa
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path path = jarFile.getParent();
        implement(token, path);
        compile(token, path);
        makeJar(token, path, jarFile);
    }

    /**
     * Compiles given class with dependencies.
     * @param token class to compile
     * @param path root path
     * @throws ImplerException if the program failed to compile
     */
    private void compile(Class<?> token, Path path) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String[] whatIWantToCompile = {"-encoding" , "UTF8", makeNewFileByPath(token, path).toString(), "-cp", getClassPath(token)};
        final int result = compiler.run(null, null, null, whatIWantToCompile);
        if (result != 0) {
            throw new ImplerException("Failed to compile");
        }
    }

    /**
     * Make jar file. Create archive in root directory.
     * @param token main class in archive
     * @param path root directory
     * @param jarFile location of archive
     * @throws ImplerException if archive can't be created
     */
    private void makeJar(Class<?> token, Path path, Path jarFile) throws ImplerException {
        try (final var writer = new JarOutputStream(Files.newOutputStream(jarFile), new Manifest())) {
            writer.putNextEntry(new ZipEntry(token.getPackageName()
                    .replace(DOT, FRONT_SLASH)
                    .concat(FRONT_SLASH + makeClassImpl(token) + ".class")));
            Files.copy(makeNewFileByPath(token, path).resolveSibling(makeClassImpl(token) + ".class"), writer);
        } catch (IOException e) {
            throw new ImplerException("Can't make Jar file: " + e.getMessage(), e);
        }
    }

    /**
     * Create -cp for compile.
     * @param clazz class
     * @return classpath
     * @throws ImplerException if wrong path was given
     */
    private static String getClassPath(Class<?> clazz) throws ImplerException {
        try {
            return Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Wrong path", e);
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkIfTokenIsCorrect(token);
        makeJavaFileByIntroducedInterface(token, root);
    }

    /**
     * Check if class is public and is an interface.
     * @param token class to check
     * @throws ImplerException if not an interface or it is private
     */
    private void checkIfTokenIsCorrect(Class<?> token) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("It is not an interface");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Interface must be public");
        }
    }

    /**
     * Make java file, which will implement given interface.
     * <p>Find path by {@link #makeNewFileByPath(Class, Path)}, where file will locate and create it.
     * Then creates the file interior with {@link #makeClassPackage(Class)}, {@link #makeClassName(Class)},
     * {@link #makeClassMethods(Class)} and write it in</p>
     * @param token interface, whom will implement
     * @param root root directory
     * @throws ImplerException if file can't be created
     */
    private void makeJavaFileByIntroducedInterface(Class<?> token, Path root) throws ImplerException {
        Path fileName = makeNewFileByPath(token, root);
        try (final var bufferedWriter = Files.newBufferedWriter(fileName)) {
            bufferedWriter.write(makeClassPackage(token) +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    makeClassName(token) +
                    makeClassMethods(token));
        } catch (IOException e) {
            throw new ImplerException("Can't create file: " + e.getMessage(), e);
        }
    }

    /**
     * Create path to the file.
     * <p>Takes root path and Interface package, concat them like a path, then check,
     * if file can be created</p>
     * @param token given Interface
     * @param root root directory
     * @return path to the file
     * @throws ImplerException if file can't be created
     */
    private Path makeNewFileByPath(Class<?> token, Path root) throws ImplerException {
        String o1 = root + File.separator;
        String o2 = token.getPackageName().replace(DOT, File.separatorChar) + File.separator;
        String o3 = makeClassImpl(token) + ".java";
        File file = new File(o1 + o2 + o3);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new ImplerException("File can't be created");
            }
        }
        try {
            return Path.of(o1 + o2 + o3);
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path" + e.getMessage(), e);
        }
    }

    /**
     * Makes class package
     * <p>Generate package by adding to {@code package}  {@link Class#getPackageName()}</p>
     * @param token given interface
     * @return String, which contains package
     */
    private String makeClassPackage(Class<?> token) {
        if (Objects.equals(token.getPackage(), null)) {
            return "";
        }
        return "package " + token.getPackageName() + SEMI_COLON;
    }

    /**
     * Makes class name
     * <p>Generate Class name with {@link #makeClassImpl(Class)} and {@link Class#getCanonicalName()}</p>
     * @param token given interface
     * @return String cantains classname
     */
    private String makeClassName(Class<?> token) {
        return String.format("public class %s implements %s {\n\n", makeClassImpl(token), token.getCanonicalName());
    }

    /**
     * Make class implemented.
     * <p>Generate new classname by adding to {@link Class#getSimpleName()} {@code Impl}</p>
     * @param token given Interface
     * @return new classname
     */
    private String makeClassImpl(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Reformat methods for given interface.
     * <p>Takes methods from interface with {@link Class#getMethods()} and reformat
     * then with {@link #makeClassMethodsImpl(Method)}</p>
     * @param token given interface
     * @return String with reformat methods
     */
    private String makeClassMethods(Class<?> token) {
        Method[] methods = token.getMethods();
        StringBuilder javaMethods = new StringBuilder();
        for (Method method : methods) {
            javaMethods.append(makeClassMethodsImpl(method));
        }
        javaMethods.append("}");
        return javaMethods.toString();
    }

    /**
     * Reformat method from interface for class, which will implement it.
     * <p>It will reformat it with special function:
     * {@link Method#getReturnType()},
     * {@link #getMethodParameters(Method)}, {@link #getMethodException(Method)},
     * {@link #checkReturnType(Class)}
     * </p>
     * @param method given method
     * @return String with new method
     */
    private String makeClassMethodsImpl(Method method) {
        return String.format("\tpublic %s %s(%s) %s {\n\t\t%s;\n\t}\n\n",
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getMethodParameters(method),
                getMethodException(method),
                checkReturnType(method.getReturnType()));
    }

    /**
     * Takes from method exceptions
     * @param method given method
     * @return String with exceptions method throw
     */
    private String getMethodException(Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        return exceptionTypes.length > 0 ? "throws " + Arrays.stream(exceptionTypes)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ")) : "";
    }

    /**
     * Takes from method parameters
     * @param method given method
     * @return String with method parameters
     */
    private String getMethodParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        return parameters.length > 0 ? Arrays.stream(method.getParameters())
                .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ")) : "";
    }

    /**
     * Takes class returnType
     * @param returnType class
     * @return String with default return type
     */
    private String checkReturnType(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return "return null";
        } else if (Objects.equals(returnType, Boolean.TYPE)) {
            return "return false";
        } else if (Objects.equals(returnType, Void.TYPE)) {
            return "return";
        } else {
            return "return 0";
        }
    }

    /**
     * The entry point of program.
     * <p>If there is one argument, runs the {@link #implement(Class, Path)} method, which will make Java class
     * implemented given interface, otherwise if there 3 arguments it will run
     * {@link #implementJar(Class, Path)} (Class, Path)} and make jar file</p>
     * @param args provided arguments
     */
    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Args are null");
            return;
        }
        if (args.length != 1 && args.length != 3) {
            System.err.println("Incorrect args");
            return;
        }
        if (args[0] == null) {
            System.err.println("Some elements of args array are null");
            return;
        }
        Path path = Path.of(System.getProperty("user.dir"));
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3) {
                Class<?> clazz = Class.forName(args[1]);
                implementor.implementJar(clazz, Path.of(args[2]));
            } else {
                Class<?> clazz = Class.forName(args[0]);
                implementor.implement(clazz, path);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Class can not be implemented: " + e.getMessage());
        }
    }
}
