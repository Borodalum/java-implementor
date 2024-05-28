package info.kgeorgiy.ja.grishin.implementor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Code generator for java interfaces with default implementation of methods.
 * <p>
 * Implements {@link info.kgeorgiy.ja.grishin.implementor.ImplGenerator}.
 * @see info.kgeorgiy.ja.grishin.implementor.ImplGenerator
 *
 * @author brrdlam
 */
public class DefaultImplGenerator implements ImplGenerator {
    /**
     * Class token of implemented interface
     */
    private Class<?> classToken;

    /**
     * Enum for all string tokens, necessary for generating code.
     */
    private enum Tokens {
        /**
         * Token for package java key-word.
         */
        PACKAGE("package"),
        /**
         * Token for java expression end.
         */
        EXPR_END(";"),
        /**
         * System-dependent line seperator
         * @see System#lineSeparator()
         */
        LINE_SEPARATOR(System.lineSeparator()),
        /**
         * Token for java public modifier.
         */
        PUBLIC("public"),
        /**
         * Token for java interface class-type.
         */
        INTERFACE("interface"),
        /**
         * Token for java class class-type.
         */
        CLASS("class"),
        /**
         * Token for space.
         */
        SPACE(" "),
        /**
         * Token for java classes implements interfaces.
         */
        IMPLEMENTS("implements"),
        /**
         * Token for java classes inherits classes.
         */
        EXTENDS("extends"),
        /**
         * Token for name of implemented interfaces.
         */
        IMPL("Impl"),
        /**
         * Left brace of java expressions.
         */
        LEFT_BRACE("{"),
        /**
         * Right brace of java expressions.
         */
        RIGHT_BRACE("}"),
        /**
         * Token for tabs.
         */
        TAB("\t"),
        /**
         * Left brace of java parenthesis.
         */
        LEFT_PARENTHESIS("("),
        /**
         * Right brace of java parenthesis.
         */
        RIGHT_PARENTHESIS(")"),
        /**
         * Token for comma.
         */
        COMMA(","),
        /**
         * Token for methods throws exceptions.
         */
        THROWS("throws"),
        /**
         * Token for java return.
         */
        RETURN("return"),
        /**
         * Token for java "true" boolean value.
         */
        TRUE("true"),
        /**
         * Token for java "false" boolean value.
         */
        FALSE("false"),
        /**
         * Token for java "null" value.
         */
        NULL("null"),
        /**
         * Token for java "0" value.
         */
        ZERO("0");

        /**
         * Token string value.
         */
        private final String value;

        /**
         * The way to create new token with specific string value.
         * @param value token string value
         */
        Tokens(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * {@link DefaultImplGenerator} default constructor.
     */
    public DefaultImplGenerator() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateImpl(final Class<?> token, final Path path) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Token must be an interface.");
        } else {
            int modifier = token.getModifiers();
            if (Modifier.isPrivate(modifier)) {
                throw new ImplerException("Interface isn't accessible.");
            }
        }
        classToken = token;
        final String code = generateInterface();
        writeCodeToFile(code, path);
    }

    /**
     * Method which generating interfaces implementations.
     * @return Generated interface.
     */
    private String generateInterface() {
        return generatePackage()
                + generateClassName()
                + Tokens.LEFT_BRACE + Tokens.LINE_SEPARATOR
                + generateMethods()
                + Tokens.RIGHT_BRACE + Tokens.LINE_SEPARATOR;
    }

    /**
     * Method which generating "package line".
     * @return Generated "package line" with package key-word, and it's name.
     */
    private String generatePackage() {
        if (classToken.getPackageName().isEmpty()) {
            return "";
        }
        return Tokens.PACKAGE.toString() + Tokens.SPACE + classToken.getPackageName() + Tokens.EXPR_END
                + Tokens.LINE_SEPARATOR;
    }

    /**
     * Method which generating "class line".
     * @return Generated "class line" with class key-word, and it's name.
     */
    private String generateClassName() {
        return Tokens.PUBLIC.toString() + Tokens.SPACE + Tokens.CLASS + Tokens.SPACE
                + classToken.getSimpleName() + Tokens.IMPL +
                Tokens.SPACE + Tokens.IMPLEMENTS + Tokens.SPACE + classToken.getCanonicalName() + Tokens.SPACE;
    }

    /**
     * Method which generating methods implementations.
     * @return Generated methods.
     */
    private String generateMethods() {
        StringBuilder methods = new StringBuilder();
        MethodGenerator methodGenerator = new MethodGenerator();
        for (final Method method : classToken.getMethods()) {
            methods.append(methodGenerator.generateMethod(method));
        }
        return methods.toString();
    }

    /**
     * Method which writing implemented interface to file.
     * @param code generated code to write in
     * @param path path to store implemented interface in.
     * @throws ImplerException throws when writing to file failed.
     */
    private void writeCodeToFile(final String code, final Path path) throws ImplerException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            StringBuilder sb = new StringBuilder();
            for (char character : code.toCharArray()) {
                if (character < 128) {
                    sb.append(character);
                } else {
                    sb.append(String.format("\\u%04x", (int)character));
                }
            }
            bufferedWriter.write(sb.toString());
        } catch (final IOException err) {
            throw new ImplerException("Implementation failed.", err);
        }
    }

    /**
     * Class to generate method implementations.
     */
    private static class MethodGenerator {
        /**
         * Method to generate.
         */
        private Method method;
        /**
         * Method return type class token.
         */
        private Class<?> returnType;

        /**
         * {@link MethodGenerator} default constructor.
         */
        public MethodGenerator() {

        }

        /**
         * Main method of class to generate methods implementations.
         * @param method method to generate implementation.
         * @return Generated method implementation.
         */
        public String generateMethod(Method method) {
            this.method = method;
            this.returnType = method.getReturnType();
            return Tokens.TAB + generateSignature() + Tokens.SPACE
                    + Tokens.LEFT_BRACE + Tokens.LINE_SEPARATOR
                    + generateReturn()
                    + Tokens.TAB + Tokens.RIGHT_BRACE
                    + Tokens.LINE_SEPARATOR;
        }

        /**
         * Method generating method signature
         * @return Generated method signature.
         */
        private String generateSignature() {
            final String modifier = Modifier.toString(method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
            return modifier + Tokens.SPACE + returnType.getCanonicalName() + Tokens.SPACE
                    + method.getName() + generateArgs() + generateExceptions();
        }

        /**
         * Method generating method arguments.
         * @return Generated methods arguments.
         */
        private String generateArgs() {
            final StringBuilder args = new StringBuilder();
            args.append(Tokens.LEFT_PARENTHESIS);
            final Parameter[] params = method.getParameters();
            if (params.length > 0) {
                for (int i = 0; i < params.length - 1; i++) {
                    args.append(params[i].getType().getCanonicalName());
                    args.append(Tokens.SPACE);
                    args.append(params[i].getName());
                    args.append(Tokens.COMMA).append(Tokens.SPACE);
                }
                args.append(params[params.length - 1].getType().getCanonicalName());
                args.append(Tokens.SPACE);
                args.append(params[params.length - 1].getName());
            }
            args.append(Tokens.RIGHT_PARENTHESIS);
            return args.toString();
        }

        /**
         * Method generating "throws" statement with exceptions.
         * @return Generated "throws" statement with exceptions.
         */
        private String generateExceptions() {
            final Class<?>[] exceptions = method.getExceptionTypes();
            if (exceptions.length > 0) {
                final StringBuilder exceptionsBuilder = new StringBuilder();
                exceptionsBuilder.append(Tokens.THROWS).append(Tokens.SPACE);
                for (int i = 0; i < exceptions.length - 1; i++) {
                    exceptionsBuilder.append(exceptions[i].getCanonicalName());
                    exceptionsBuilder.append(Tokens.COMMA);
                }
                exceptionsBuilder.append(exceptions[exceptions.length - 1].getCanonicalName());
                return Tokens.SPACE + exceptionsBuilder.toString();
            }
            return "";
        }

        /**
         * Method generating return statement with default value.
         * @return Generated return statement with default value.
         */
        private String generateReturn() {
            if (returnType == void.class) {
                return "";
            }

            final String defaultValue;
            if (returnType == boolean.class) {
                defaultValue = Tokens.FALSE.toString();
            } else if (returnType.isPrimitive()) {
                defaultValue = Tokens.ZERO.toString();
            } else {
                defaultValue = Tokens.NULL.toString();
            }
            return Tokens.TAB.toString() + Tokens.TAB + Tokens.RETURN + Tokens.SPACE + defaultValue + Tokens.EXPR_END + Tokens.LINE_SEPARATOR;
        }
    }
}
