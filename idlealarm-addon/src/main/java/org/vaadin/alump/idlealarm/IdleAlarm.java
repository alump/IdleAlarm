package org.vaadin.alump.idlealarm;

import com.vaadin.server.AbstractExtension;
import com.vaadin.server.Extension;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.UI;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

/**
 * Allows to define idle timeout warning shown on client side before session expires because of long idle period
 */
public class IdleAlarm extends AbstractExtension {

    public static final String DEFAULT_FORMATTING = "Your session will expire in less than "
            + IdleAlarmFormatting.SECS_TO_TIMEOUT + " seconds";

    protected IdleAlarm(UI ui) {
        setMessage(DEFAULT_FORMATTING);
        int maxInactiveInterval = IdleTimeoutServerUtil.resolveMaxInactiveInterval(ui);

        extend(ui);
        getState().maxInactiveInterval = maxInactiveInterval;
        if(maxInactiveInterval <= getState().secondsBefore) {
            int value = maxInactiveInterval - 5;
            setSecondsBefore(value > 0 ? value : 1);
        }

        // Register dummy implementation to allow reset timeout calls
        registerRpc(new ResetTimeoutServerRpc() {
            @Override
            public void resetIdleTimeout() {
                //ignored, call is just to reset timeout
            }
        });
    }

    @Override
    protected IdleAlarmState getState() {
        return (IdleAlarmState)super.getState();
    }

    /**
     * Get instance of IdleAlarm. Instance will be automatically created and initialized if required. This method can be
     * only used inside UI context.
     * @return Instance of IdleAlarm
     * @throws IllegalArgumentException If UI can not be resolved
     * @throws IllegalStateException If given UI does not have valid max inactive interval defined
     */
    public static IdleAlarm get() throws IllegalArgumentException, IllegalStateException {
        return get(UI.getCurrent());
    }

    /**
     * Get instance of IdleAlarm. I
     * @param ui UI that is extended
     * @return Instance of IdleAlarm
     * @throws IllegalArgumentException If invalid UI given
     * @throws IllegalStateException If given UI does not have valid max inactive interval defined
     */
    public static IdleAlarm get(UI ui) throws IllegalArgumentException, IllegalStateException {
        if(ui == null) {
            throw new IllegalArgumentException("UI can not be null");
        }

        for(Extension extension : ui.getExtensions()) {
            if(extension instanceof IdleAlarm) {
                return (IdleAlarm)extension;
            }
        }

        return new IdleAlarm(ui);
    }

    /**
     * Removes IdleAlarm extension from current UI context
     */
    public static void unload() {
        unload(UI.getCurrent());
    }

    /**
     * Removes IdleAlarm extension from given UI content
     * @param ui UI from where extension is unloaded
     */
    public static void unload(UI ui) {
        if(ui == null) {
            throw new IllegalArgumentException("UI can not be null");
        }

        IdleAlarm idleAlarm = null;
        for(Extension extension : ui.getExtensions()) {
            if(extension instanceof IdleAlarm) {
                idleAlarm = (IdleAlarm)extension;
                break;
            }
        }
        if(idleAlarm != null) {
            ui.removeExtension(idleAlarm);
        }
    }

    /**
     * Set how many seconds before timeout warning will be shown
     * @param seconds Time in seconds
     * @return IdleAlarm instance to allow chaining of commands
     * @throws java.lang.IllegalArgumentException If given value is smaller than 1 seconds AND/OR bigger than idle
     * timeout value resolved from VaadinSession.
     */
    public IdleAlarm setSecondsBefore(int seconds) throws IllegalArgumentException {
        if(seconds < 1) {
            throw new IllegalArgumentException("Invalid amount of seconds (" + seconds + ") given");
        } else if(seconds >= getState().maxInactiveInterval) {
            throw new IllegalArgumentException("Given value " + seconds + " is larger or equal to timeout value "
                    + getState().maxInactiveInterval);
        }
        getState().secondsBefore = seconds;
        return this;
    }

    /**
     * Get how many seconds before timeout warning will be shown
     * @return Time in seconds
     */
    public int getSecondsBefore() {
        return getState().secondsBefore;
    }

    /**
     * Get value resolved from VaadinSession
     * @return Idle timeout in seconds
     */
    public int getMaxInactiveInternal() {
        return getState().maxInactiveInterval;
    }

    /**
     * Set message shown in idle timeout warning
     * @param message Message shown in idle timeout warning
     * @return IdleAlarm instance to allow chaining of commands
     */
    public IdleAlarm setMessage(String message) {
        if(message == null) {
            throw new IllegalArgumentException("Message can not be null");
        }
        getState().message = message;
        return this;
    }

    public ContentMode getContentMode() {
        return getState().contentMode;
    }

    public IdleAlarm setContentMode(ContentMode contentMode) {
        if(contentMode == null) {
            throw new IllegalArgumentException("Content mode can not be null");
        }
        getState().contentMode = contentMode;
        return this;
    }

    /**
     * Get message shown in idle timeout warning
     * @return Message shown in idle timeout warning
     */
    public String getMessage() {
        return getState().message;
    }
}
