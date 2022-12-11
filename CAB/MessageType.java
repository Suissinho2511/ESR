package CAB;

public enum MessageType {
    // Generic message
    HELLO,
    // To decide which server sends the fastest package
    // Say a description of how it works
    PROBE_PATH,
    // To say which server??
    // Say a description of how it works
    REPLY_PATH;

    public static MessageType fromInteger(int x) {
        return switch (x) {
            case 0 -> HELLO;
            case 1 -> PROBE_PATH;
            case 2 -> REPLY_PATH;
            default -> null;
        };
    }

    public static int toInteger(MessageType x) {
        return switch (x) {
            case HELLO -> 0;
            case PROBE_PATH -> 1;
            case REPLY_PATH -> 2;
            default -> -1;
        };
    }
}
