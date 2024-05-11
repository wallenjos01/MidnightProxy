package org.wallentines.mdproxy;

public interface Task {


    String PRE_LOGIN_QUEUE = "pre_login";
    String POST_LOGIN_QUEUE = "post_login";
    String CONFIGURE_QUEUE = "configure";

    void run(String queue, ClientConnection connection);

}
