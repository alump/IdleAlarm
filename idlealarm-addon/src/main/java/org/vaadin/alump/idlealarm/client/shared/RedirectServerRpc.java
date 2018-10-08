package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.communication.ServerRpc;

/**
 * Redirect RPC calls from client to server
 */
public interface RedirectServerRpc extends ServerRpc {

    /**
     * Redirect happened
     */
    void redirected();

}
