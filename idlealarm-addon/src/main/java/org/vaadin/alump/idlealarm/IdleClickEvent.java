package org.vaadin.alump.idlealarm;

import com.vaadin.event.ConnectorEvent;
import com.vaadin.shared.MouseEventDetails;

import java.util.Objects;

/**
 * Event called when application defined button has been clicked
 */
public class IdleClickEvent extends ConnectorEvent {

    private final Object clickedButtonID;
    private final MouseEventDetails details;

    public IdleClickEvent(IdleAlarm idleAlarm, MouseEventDetails details, Object clickedButtonID) {
        super(idleAlarm);
        this.details = details;
        this.clickedButtonID = Objects.requireNonNull(clickedButtonID);
    }

    public IdleAlarm getIdleAlarm() {
        return (IdleAlarm)super.getConnector();
    }

    public Object getClickedButtonID() {
        return clickedButtonID;
    }

    public MouseEventDetails getDetails() {
        return details;
    }
}
