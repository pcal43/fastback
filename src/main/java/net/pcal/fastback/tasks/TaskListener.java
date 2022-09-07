package net.pcal.fastback.tasks;

public interface TaskListener {

    void feedback(String message);

    void error(String message);

    default void internalError() {
        error("An unexpected error occurred. See log for details.");
    }


    public static class NoListener implements TaskListener {

        @Override
        public void feedback(String message) {

        }

        @Override
        public void error(String message) {

        }
    }
}
