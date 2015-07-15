package org.vaadin.alump.idlealarm;

import com.vaadin.ui.UI;

/**
 * Server side util
 */
public class IdleTimeoutServerUtil {

    /**
     * Resolves max inactive interval from UI
     * @param ui UI used to resolve value
     * @return Interval in seconds
     * @throws IllegalArgumentException If invalid UI given
     * @throws IllegalStateException If given UI does not have valid max inactive interval defined
     */
    public static int resolveMaxInactiveInterval(UI ui) throws IllegalArgumentException, IllegalStateException {
        if(ui == null) {
            throw new IllegalArgumentException("UI can not be null");
        }

        if(!ui.getSession().getService().getDeploymentConfiguration().isCloseIdleSessions()) {
            throw new IllegalStateException("Idle sessions are not closed. IdleAlarm can not be used.");
        }

        int maxInactiveInterval = ui.getSession().getSession().getMaxInactiveInterval();
        if(maxInactiveInterval < 1) {
            throw new IllegalStateException("MaxInactiveInterval " + maxInactiveInterval + " is not supported by IdleAlarm");
        }

        return maxInactiveInterval;
    }
}
