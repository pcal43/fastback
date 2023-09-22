package net.pcal.fastback.utils;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Thrown when an attempt to execute an external process fails.
 *
 * @author pcal
 * @since 0.15.0
 */
public class ProcessException extends Exception {
    private final List<String> processOutput;

    ProcessException(String[] args, final int exitCode, final List<String> processOutput, Throwable nested) {
        super("Exit " + exitCode + " when executing: " + String.join(" ", args), nested);
        this.processOutput = requireNonNull(processOutput);
    }

    ProcessException(String[] args, final int exitCode, final List<String> stdoutLines) {
        super("Exit " + exitCode + " when executing: " + String.join(" ", args));
        this.processOutput = requireNonNull(stdoutLines);
    }

    /**
     * Copies the original process to the given line consumer.
     */
    public void writeProcessOutput(Consumer<String> consumer) {
        for (String line : processOutput) consumer.accept(line);
    }
}
