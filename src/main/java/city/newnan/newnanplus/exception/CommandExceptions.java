package city.newnan.newnanplus.exception;

public class CommandExceptions extends Exception {
    public static class NoPermissionException extends CommandExceptions {
        public static String message;
    }
    public static class PlayerOfflineException extends Exception {
        public static String message;
    }
    public static class PlayerNotFountException extends Exception {
        public static String message;
    }
    public static class PlayerMoreThanOneException extends Exception {
        public static String message;
    }
    public static class RefuseConsoleException extends Exception {
        public static String message;
    }
    public static class BadUsageException extends Exception {
        public static String message;
    }
    public static class NoSuchCommandException extends Exception {
        public static String message;
    }
    public static class OnlyConsoleException extends Exception {
        public static String message;
    }
    public static class CustomCommandException extends Exception {
        public String reason;
        public static String message;
        public CustomCommandException(String reason) {
            this.reason = reason;
        }
    }
}
