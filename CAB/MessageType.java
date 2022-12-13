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
    OPTIN,
    // For when a client wants to optout
    OPTOUT;

    public static MessageType fromInteger(int x) {
        switch (x) {
            case 0: return HELLO;
            case 1: return CHOOSE_SERVER;
            case 2: return REPLY_CHOOSE_SERVER;
            case 3: return TOPOLOGY;
            case 4: return REPLY_TOPOLOGY;
            case 5: return OPTIN;
            case 6: return OPTOUT;
        };
		return null;
    }

    public static int toInteger(MessageType x) {
        switch (x) {
            case HELLO: return 0;
            case CHOOSE_SERVER: return 1;
            case REPLY_CHOOSE_SERVER: return 2;
            case TOPOLOGY: return 3;
            case REPLY_TOPOLOGY: return 4;
            case OPTIN: return 5;
            case OPTOUT: return 6;
        };
		return -1;
    }
}
