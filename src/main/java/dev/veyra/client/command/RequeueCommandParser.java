package dev.veyra.client.command;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts a sanitized Hypixel /locraw response into a BedWars play command. */
final class RequeueCommandParser {
    private static final Pattern GAME_TYPE = jsonStringField("gametype");
    private static final Pattern MODE = jsonStringField("mode");
    private static final Pattern SERVER = jsonStringField("server");
    private static final Pattern SAFE_MODE = Pattern.compile("[A-Z0-9_]{1,64}");

    private RequeueCommandParser() {
    }

    static Result fromLocraw(String message) {
        if (message == null || !message.trim().startsWith("{")) {
            return Result.notHandled();
        }

        String server = field(SERVER, message);
        String gameType = field(GAME_TYPE, message);
        String mode = field(MODE, message);
        if (server == null || gameType == null || mode == null) {
            return Result.notHandled();
        }
        if (!"BEDWARS".equalsIgnoreCase(gameType)) {
            return Result.handled(null);
        }

        String normalizedMode = mode.toUpperCase(Locale.ROOT);
        if (!SAFE_MODE.matcher(normalizedMode).matches()) {
            return Result.handled(null);
        }
        if (!normalizedMode.startsWith("BEDWARS_")) {
            normalizedMode = "BEDWARS_" + normalizedMode;
        }
        return Result.handled("/play " + normalizedMode.toLowerCase(Locale.ROOT));
    }

    private static Pattern jsonStringField(String name) {
        return Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    }

    private static String field(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    static final class Result {
        private static final Result NOT_HANDLED = new Result(false, null);

        private final boolean handled;
        private final String playCommand;

        private Result(boolean handled, String playCommand) {
            this.handled = handled;
            this.playCommand = playCommand;
        }

        static Result notHandled() {
            return NOT_HANDLED;
        }

        static Result handled(String playCommand) {
            return new Result(true, playCommand);
        }

        boolean handled() {
            return handled;
        }

        String playCommand() {
            return playCommand;
        }
    }
}
