package info.kgeorgiy.ja.grishin.implementor;

import java.nio.file.Path;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Interface for implementation code generators
 *
 * @author brrdlam
 */
public interface ImplGenerator {
    /**
     * The main method to generate implementation of given interface.
     * @param token class token to implement
     * @param pathToClass path to implemented class
     * @throws ImplerException throws when something went wrong while implementing interface.
     */
    void generateImpl(Class<?> token, Path pathToClass) throws ImplerException;
}
