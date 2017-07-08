package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.communication.ServerRpc;

/**
 * Server RPC of interface for IdleAlarm
 */
public interface IdleAlarmServerRpc extends ServerRpc {

    void resetIdleTimeout();

    void buttonClicked(int id, MouseEventDetails details);

}
