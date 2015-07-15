package org.vaadin.alump.idlealarm.client.shared;

/**
 * Created by alump on 15/07/15.
 */
public interface IdleAlarmFormatting {

    /**
     * This part of message will be replaced with seconds left to next timeout
     */
    String SECS_TO_TIMEOUT = "%SECS_TO_TIMEOUT%";

    /**
     * This part of message will be replaced with seconds since timeout reset last time
     */
    String SECS_SINCE_RESET = "%SECS_SINCE_RESET%";

    /**
     * How many seconds client thinks sessions can stay idle
     */
    String SECS_MAX_IDLE_TIMEOUT = "%SECS_MAX_IDLE_TIMEOUT%";
}
