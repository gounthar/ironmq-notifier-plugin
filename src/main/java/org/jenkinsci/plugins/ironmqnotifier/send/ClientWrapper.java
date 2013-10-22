package org.jenkinsci.plugins.ironmqnotifier.send;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;


public class ClientWrapper extends Client {

    public ClientWrapper ( String projectId, String token, Cloud cloud ) {
        super (projectId, token, cloud);
    }

    //
    //This module is created to generate appropriate exceptions early  BEFORE attempting to send
    //

    public Client GenerateClientToUse ( String projectId, String token, Cloud cloud ) {

            return new Client(projectId, token, cloud);

    }
}
