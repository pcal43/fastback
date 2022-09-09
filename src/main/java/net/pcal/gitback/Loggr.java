package net.pcal.gitback;

import java.io.IOException;
import java.util.function.Supplier;

public interface Loggr {

    void error(String message);

    void error(Throwable t);

    void error(String message, Throwable t);

    void warn(String message);

    void warn(String s, IOException ioe);

    void info(String message);

    void debug(String message);

    void debug(Supplier<?> messageSupplier);

    void trace(String message);

    void trace(Supplier<?> messageSupplier);

    void trace(Supplier<?> s, Throwable t);
}