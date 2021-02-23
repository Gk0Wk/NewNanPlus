package city.newnan.newnanplus.exception;

import city.newnan.newnanplus.NewNanPlus;

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
        public AccessFileErrorException(String who) { this.who = who; }
    }
    public static class CustomCommandException extends CommandExceptions {
        public String reason;
        public static String message;
        public CustomCommandException(String reason)
        {
            this.reason = reason;
        }
    }

    public static void init(NewNanPlus plugin)
    {
        AccessFileErrorException.message = plugin.languageManager.provideLanguage("&c$global_message.access_file_error$");
        BadUsageException.message = plugin.languageManager.provideLanguage("&c$global_message.bad_usage$");
        CustomCommandException.message = plugin.languageManager.provideLanguage("&c$global_message.custom_command_error$");
        NoSuchCommandException.message = plugin.languageManager.provideLanguage("&c$global_message.no_such_command$");
        NoPermissionException.message = plugin.languageManager.provideLanguage("&c$global_message.no_permission$");
        OnlyConsoleException.message = plugin.languageManager.provideLanguage("&c$global_message.only_console$");
        PlayerMoreThanOneException.message = plugin.languageManager.provideLanguage("&c$global_message.find_more_than_one_player$");
        PlayerNotFountException.message = plugin.languageManager.provideLanguage("&c$global_message.player_not_found$");
        PlayerOfflineException.message = plugin.languageManager.provideLanguage("&c$global_message.player_offline$");
        RefuseConsoleException.message = plugin.languageManager.provideLanguage("&c$global_message.console_selfrun_refuse$");
    }
}
