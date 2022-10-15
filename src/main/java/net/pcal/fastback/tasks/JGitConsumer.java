package net.pcal.fastback.tasks;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Consumer with typed exceptions for typical JGit operations.
 */
@FunctionalInterface
interface JGitConsumer<T> {

    void accept(T t) throws IOException, GitAPIException;
}
