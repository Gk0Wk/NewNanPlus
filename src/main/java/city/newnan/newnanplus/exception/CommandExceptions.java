package city.newnan.newnanplus.exception;

public class CommandExceptions {
    public static class NoPermissionException extends Exception { }
    public static class PlayerOfflineException extends Exception { }
    public static class RefuseConsoleException extends Exception { }
    public static class BadUsageException extends Exception { }
    public static class NoSuchCommandException extends Exception { }
    public static class OnlyConsoleException extends Exception { }
}
