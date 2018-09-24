package org.vaadin.alump.idlealarm.client.shared;

import com.vaadin.shared.communication.SharedState;
import com.vaadin.shared.ui.label.ContentMode;

import java.util.ArrayList;
import java.util.List;

/**
 * State of IdleAlarm extension
 */
public class IdleAlarmState extends SharedState {

    public int maxInactiveInterval = -1;

    public int secondsBefore = 60;

    public String message = "Your session will expire soon";

    public ContentMode contentMode = ContentMode.TEXT;

    public boolean liveTimeoutSecondsEnabled;

    public String timeoutRedirectURL;

    public boolean closeEnabled = false;

    public boolean redirectEnabled = false;

    public boolean refreshEnabled = false;

    public String closeCaption = "Close";

    public String redirectCaption = "Redirect";

    public String refreshCaption = "Refresh";

    public TimeoutAction timeoutAction = TimeoutAction.DEFAULT;

    public List<String> styleNames = new ArrayList<String>();
}
