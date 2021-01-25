package ru.gb_students.the_chat.library;

public class Protocol {
    public static final String DELIMITER = "±";
    public static final String AUTH_REQUEST = "/auth_request";
    public static final String AUTH_ACCEPT = "/auth_accept";
    public static final String AUTH_DENIED = "/auth_denied";
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";
    public static final String TYPE_BROADCAST = "/bcast";
    public static final String USER_LIST = "/user_list";
    public static final String USER_BROADCAST = "/user_bcast";
    public static final String CHANGE_NICKNAME = "/change_nickname";
    public static final String CHANGE_NICKNAME_FAILED = "/changed_nickname_failed";
    public static final String MESSAGE_LIST = "/message_list";
    public static final String REQUEST_MESSAGE_LIST = "/request_message_list";

    public static String getUserBroadcast(String msg) {
        return USER_BROADCAST + DELIMITER + msg;
    }

    public static String getMessageList(String messages) {
        return MESSAGE_LIST + DELIMITER + messages;
    }

    public static String getRequestMessageList() {
        return REQUEST_MESSAGE_LIST;
    }

    public static String getUserList(String users) {
        return USER_LIST + DELIMITER + users;
    }

    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }

    public static String getChangeNickname(String newNickname) {
        return CHANGE_NICKNAME + DELIMITER + newNickname;
    }

    public static String getChangeNicknameFailed() {
        return CHANGE_NICKNAME_FAILED;
    }

    public static String getAuthAccept(String nickname) {
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getAuthDenied() {
        return AUTH_DENIED;
    }

    public static String getMsgFormatError(String message) {
        return MSG_FORMAT_ERROR + DELIMITER + message;
    }

    public static String getTypeBroadcast(String src, String message) {
        return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() +
                DELIMITER + src + DELIMITER + message;
    }
}
