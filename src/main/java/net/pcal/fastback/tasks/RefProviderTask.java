package net.pcal.fastback.tasks;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.util.Collection;
import java.util.concurrent.Callable;

public interface RefProviderTask extends Callable<Collection<Ref>> {

    Collection<Ref> call() throws GitAPIException;
}
