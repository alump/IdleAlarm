package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ui.ContentMode;

/**
 * State of idle countdown label
 */
public class IdleCountdownLabelState extends AbstractComponentState {

    public static final String DEFAULT_FORMATTING = "";

    {
        primaryStyleName = "idle-countdown-label";
    }

    public int maxInactiveInterval = -1;

    public String formatting = IdleAlarmFormatting.SECS_TO_TIMEOUT;

    public ContentMode contentMode = ContentMode.TEXT;

}
