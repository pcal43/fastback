package net.pcal.gitback.tasks;

public interface TaskListener {

    void feedback(String message);

    void broadcast(String message);

    void error(String message);

    default void internalError() {
        error("An unexpected error occurred. See log for details.");
    }

    default void backupsNotEnabled() {
        error("Backups are not enabled on this world.  Run '/backup enable'");
    }


    public static class NoListener implements TaskListener {

        @Override
        public void feedback(String message) {

        }

        @Override
        public void broadcast(String message) {

        }

        @Override
        public void error(String message) {

        }
    }
}
