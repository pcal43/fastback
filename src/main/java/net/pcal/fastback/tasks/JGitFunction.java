package net.pcal.fastback.tasks;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Function with typed exceptions for typical JGit operations.
 */
@FunctionalInterface
interface JGitFunction<T, R> {

    R apply(T arg) throws IOException, GitAPIException;
}
