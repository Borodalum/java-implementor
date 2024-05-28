package info.kgeorgiy.ja.grishin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.tools.ToolProvider;

/**
 * The main class of java interface implementor.
 * <p>
 * Implements {@link JarImpler}.
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 *
 * @author brrdlam
 */
public class Implementor implements JarImpler {
    /**
     * Code generator for implemented interfaces.
     * @see ImplGenerator
     */
    private final ImplGenerator implGenerator = new DefaultImplGenerator();

    /**
     * Enum for all string tokens, necessary for generating implementation.
     */
    private enum Tokens {
        /**
         * The system-dependent "slash" token.
         * @see File#separatorChar
         */
        SLASH("/"),
        /**
         * The postfix of all implemented interfaces.
         */
        IMPL("Impl"),
        /**
         * The "dot" token.
         */
        DOT("."),
        /**
         * The java file extension.
         */
        JAVA_EXTENSION(".java"),
        /**
         * The java compiled class extension.
         */
        CLASS_EXTENSION(".class");

        /**
         * Token string value.
         */
        private final String value;

        /**
         * The way to create new token with specific string value.
         * @param value token string value
         */
        Tokens(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * {@link Implementor} default constructor.
     */
    public Implementor() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        try {
            Path pathToClass = Paths.get(path.toString() + Tokens.SLASH
                    + aClass.getPackageName().replace(Tokens.DOT.toString(), Tokens.SLASH.toString()) + Tokens.SLASH
                    + aClass.getSimpleName() + Tokens.IMPL
                    + Tokens.JAVA_EXTENSION
            );
            Files.createDirectories(pathToClass.getParent());
            implGenerator.generateImpl(aClass, pathToClass);
        } catch (final IOException err) {
            throw new ImplerException("Failed to create path to class.", err);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            final String packageName = token.getPackageName().replace(Tokens.DOT.toString(), Tokens.SLASH.toString());
            final String implClassReference = packageName + Tokens.SLASH + token.getSimpleName() + Tokens.IMPL;

            final Path implementationDir = Files.createTempDirectory(jarFile.getParent(),
                    "impl");
            implement(token, implementationDir);
            ToolProvider.getSystemJavaCompiler().run(
                    null,
                    null,
                    null,
                    "-cp",
                    jarFile + File.pathSeparator + Paths.get(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                    "-encoding",
                    StandardCharsets.UTF_8.name(),
                    (implementationDir.toString() + Tokens.SLASH + implClassReference).replace("/", File.separator) + Tokens.JAVA_EXTENSION
            );

            final Manifest manifest = new Manifest();

            try (final JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                jarOutputStream.putNextEntry(new ZipEntry(implClassReference + Tokens.CLASS_EXTENSION));

                Files.copy(Paths.get(implementationDir.toString() + Tokens.SLASH + implClassReference + Tokens.CLASS_EXTENSION), jarOutputStream);

                jarOutputStream.closeEntry();
            } catch (final IOException err) {
                throw new ImplerException("Failed to generate jar in " + jarFile);
            }
        } catch (final IOException err) {
            throw new ImplerException("Failed to create temporary directory.");
        } catch (final URISyntaxException e) {
            throw new ImplerException("Failed to provide to URI.");
        }
    }

    /**
     * The {@link Implementor} program entry.
     * @param args Expected 1 or 3 argument:
     *             "full class/interface name"
     *             or
     *             "-jar 'full class/interface name' 'output jar name'.jar".
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            final String fullClassName = args[0];
            final Implementor implementor = new Implementor();
            try {
                final Class<?> token = Class.forName(fullClassName);
                final Path path = Paths.get(token.getPackageName());

                try {
                    implementor.implement(token, path);
                } catch (final ImplerException err) {
                    System.err.println("Implementation failed.");
                    System.err.println(err.getMessage());
                }
            } catch (final ClassNotFoundException err) {
                System.err.println("Class with name \"" + fullClassName + "\" not found.");
            }
        } else if (args.length == 3) {
            if (!args[0].equals("-jar")) {
                System.err.println("Missing option \"-jar\", expected: -jar <full class/interface name> <output jar " +
                        "name>.jar");
            }
            final String fullClassName = args[1];
            final Implementor implementor = new Implementor();
            try {
                final Class<?> token = Class.forName(fullClassName);
                final Path path = Paths.get(args[2]);

                implementor.implementJar(token, path);
            } catch (final ImplerException err) {
                System.err.println("Implementation failed.");
                System.err.println(err.getMessage());
            } catch (final ClassNotFoundException err) {
                System.err.println("Class with name \"" + fullClassName + "\" not found.");
            }
        } else {
            System.err.println("Expected 1 or 3 argument: ");
            System.err.println("""
                        <full class/interface name>
                        or
                        -jar <full class/interface name> <output jar name>.jar
                    """);
        }
    }
}
