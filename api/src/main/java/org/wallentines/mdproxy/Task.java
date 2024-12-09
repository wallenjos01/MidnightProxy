package org.wallentines.mdproxy;

@Deprecated
public interface Task {

    String PRE_LOGIN_QUEUE = "pre_login";
    String POST_LOGIN_QUEUE = "post_login";
    String CONFIGURE_QUEUE = "configure";
    String PRE_BACKEND_CONNECT = "pre_backend";
    String POST_BACKEND_CONNECT = "post_backend";

    void run(String queue, ClientConnection connection);

}
