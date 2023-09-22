package net.pcal.fastback.mod.forge;

import org.eclipse.jgit.transport.SshSessionFactory;

import java.lang.reflect.InvocationTargetException;

import static net.pcal.fastback.logging.SystemLogger.syslog;

public class SshHacks {

    /**
     * This is necessary because JGit looks for it's SshSessionFactory with java.util.ServiceLoader, and that
     * just doesn't seem to be something that Forge is willing to accommodate.
     */
    public static void ensureSshSessionFactoryIsAvailable() {
        try {
            if (SshSessionFactory.getInstance() == null) {
                try {
                    final String clazz = "org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory";
                    SshSessionFactory.setInstance((SshSessionFactory) Class.forName(clazz).getConstructor().newInstance());
                    // AFAICT this only happens in Intellij.  Something about shadowJar and relocate isn't working in dev environments.
                    // Seems fine in the launcher.
                    syslog().warn("A SshSessionFactory was not located via java services; a " + clazz + " has been installed manually.  This is probably ok.");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException | InvocationTargetException ohwell) {
                    syslog().error("Unable to manually set SshSessionFactory.  SSH connections will probably not work.", ohwell);
                }
            }
            //
        } catch (Error err) {
            syslog().error("WAT", err);
        }
    }

    //JschConfigSessionFactory csf = new JschConfigSessionFactory();
    //SshSessionFactory.setInstance(csf);
    // This works fine but the jsch provider doesn't support ed25519 keys; for that we need to get mina
    // working, but that brings us into a whole other world of classloading and shading pain.  Because
    // Forge excludes everything under org.apache.*?  Is that true?  Ugh, FIXME.
    // This all works perfectly fine with Fabric. :(


    //https://stackoverflow.com/questions/67767455/setting-ssh-keys-to-use-with-jgit-with-ssh-from-apache-sshd
    //https://www.eclipse.org/forums/index.php/t/1107487/
    //https://github.com/AzBuilder/terrakube/blob/67f992c84cb2f66ce17a2e2ab85796872429720b/api/src/main/java/org/terrakube/api/plugin/ssh/TerrakubeSshdSessionFactory.java#L6

    //https://stackoverflow.com/questions/65566138/apache-mina-sshd-ssh-client-always-prints-eddsa-provider-not-supported
    //If you don't fix this, then you will not be able to validate the host keys. My testing was not impacted because I was not validating the host keys yet. However, once deployed to production, I would have been impacted because host keys must be validated.
    //https://dzone.com/articles/jgit-library-examples-in-java

    //SecurityUtils.setDefaultProviderChoice(new EdDSASecurityProviderRegistrar());

    /**
     String CLAZZ = EdDSASecurityProviderRegistrar.class.getNa        this.lifecycleListener.onInitialize();
     me();//"net.pcal.fastback.relocated.org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar";
     syslog().warn("!!!! "+CLAZZ);
     try {
     Class.forName(CLAZZ);
     } catch (ClassNotFoundException e) {
     syslog().error(new RuntimeException(e));
     }
     System.setProperty("org.apache.sshd.security.registrars", CLAZZ);
     **/
//


    /**
     *
     then we can try this, but i'm having a heck of a time getting the i2p provider to load >:(
     if (false) {
     File sshDir = new File(FS.DETECTED.userHome(), "/.ssh");

     SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
     .setPreferredAuthentications("publickey")
     .setHomeDirectory(FS.DETECTED.userHome())
     .setSshDirectory(sshDir)

     .setServerKeyDatabase((h, s) -> new ServerKeyDatabase() {

    @Override public List<PublicKey> lookup(String connectAddress,
    InetSocketAddress remoteAddress,
    Configuration config) {
    return Collections.emptyList();
    }

    @Override public boolean accept(String connectAddress,
    InetSocketAddress remoteAddress,
    PublicKey serverKey, Configuration config,
    CredentialsProvider provider) {
    return true;
    }

    })
     .build(new JGitKeyCache());
     SshSessionFactory.setInstance(sshSessionFactory);
     }
     **/
    /**

     //SshdSessionFactory factory = new SshdSessionFactory(new JGitKeyCache(), new DefaultProxyDataFactory());
     SshdSessionFactory factory = new SshdSessionFactory();
     try {
     Runtime.getRuntime()
     .addShutdownHook(new Thread(factory::close));
     } catch (IllegalStateException e) {
     // ignore - the VM is already shutting down
     }
     SshSessionFactory.setInstance(factory);


     }
     **/
}
