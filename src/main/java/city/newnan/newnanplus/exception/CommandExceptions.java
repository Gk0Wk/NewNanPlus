package city.newnan.newnanplus.exception;

public class CommandExceptions extends Exception {
    public static class NoPermissionException extends CommandExceptions {
        public static String message;
    }
    public static class PlayerOfflineException extends CommandExceptions {
        public static String message;
    }
    public static class PlayerNotFountException extends CommandExceptions {
        public static String message;
    }
    public static class PlayerMoreThanOneException extends CommandExceptions {
        public static String message;
    }
    public static class RefuseConsoleException extends CommandExceptions {
        public static String message;
    }
    public static class BadUsageException extends CommandExceptions {
        public static String message;
    }
    public static class NoSuchCommandException extends CommandExceptions {
        public static String message;
    }
    public static class OnlyConsoleException extends CommandExceptions {
        public static String message;
    }
    public static class AccessFileErrorException extends CommandExceptions {
        public static String message;
        public String who;
        public AccessFileErrorException(String who) {this.who = who;}
    }
    public static class CustomCommandException extends CommandExceptions {
        public String reason;
        public static String message;
        public CustomCommandException(String reason) {
            this.reason = reason;
        }
    }
}
