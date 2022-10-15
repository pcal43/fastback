package net.pcal.fastback.tasks;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Supplier with typed exceptions for typical JGit operations.
 */
@FunctionalInterface
interface JGitSupplier<R> {

    R get() throws IOException, GitAPIException;
}
