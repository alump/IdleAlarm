package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.communication.ServerRpc;

/**
 * Interface used to reset idle time after extensions and components of this add-on start following network
 * connectivity. This is required as any there is no way to recover information that has happened before.
 */
public interface ResetTimeoutServerRpc extends ServerRpc {

    void resetIdleTimeout();

}
