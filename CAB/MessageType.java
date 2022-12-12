package CAB;

public enum MessageType {
    // Generic message
    HELLO,
    // For cliente to choose which server has less delay
    CHOOSE_SERVER,
    REPLY_CHOOSE_SERVER,
    TOPOLOGY,
    // To say which server??
    // Say a description of how it works
    REPLY_TOPOLOGY,
    // For when a client wants to optout
    OPTOUT;

    public static MessageType fromInteger(int x) {
        return switch (x) {
            case 0 -> HELLO;
            case 1 -> CHOOSE_SERVER;
            case 2 -> REPLY_CHOOSE_SERVER;
            case 3 -> TOPOLOGY;
            case 4 -> REPLY_TOPOLOGY;
            case 5 -> OPTOUT;
            default -> null;
        };
    }

    public static int toInteger(MessageType x) {
        return switch (x) {
            case HELLO -> 0;
            case CHOOSE_SERVER -> 1;
            case REPLY_CHOOSE_SERVER -> 2;
            case TOPOLOGY -> 3;
            case REPLY_TOPOLOGY -> 4;
            case OPTOUT -> 5;
            default -> -1;
        };
    }
}
