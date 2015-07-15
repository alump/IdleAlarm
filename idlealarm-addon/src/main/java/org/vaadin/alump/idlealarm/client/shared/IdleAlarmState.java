package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.communication.SharedState;
import com.vaadin.shared.ui.label.ContentMode;

/**
 * State of IdleAlarm extension
 */
public class IdleAlarmState extends SharedState {

    public int maxInactiveInterval = -1;

    public int secondsBefore = 60;

    public String message = "Your session will expire soon";

    public ContentMode contentMode = ContentMode.TEXT;

    public String extendCaption = "Extend sessions";
}
