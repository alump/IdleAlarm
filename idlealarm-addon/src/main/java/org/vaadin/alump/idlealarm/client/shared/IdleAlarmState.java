package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.communication.SharedState;
import com.vaadin.shared.ui.ContentMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State of IdleAlarm extension
 */
public class IdleAlarmState extends SharedState {

    public static class ButtonState {
        public String caption;
        public List<String> styleNames;
    }

    public int maxInactiveInterval = -1;

    public int secondsBefore = 60;

    public String message = "Your session will expire soon";

    public ContentMode contentMode = ContentMode.TEXT;

    public boolean countdownTimeout;

    public String timeoutRedirectURL;

    public boolean closeEnabled = false;
    public String closeCaption = null;

    public TimeoutAction timeoutAction = TimeoutAction.DEFAULT;

    public List<String> styleNames = new ArrayList<>();

    public Map<Integer,ButtonState> buttons = new HashMap<>();
}
