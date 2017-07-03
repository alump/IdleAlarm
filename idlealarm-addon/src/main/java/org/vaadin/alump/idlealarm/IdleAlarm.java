package org.vaadin.alump.idlealarm;

import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.server.AbstractExtension;
import com.vaadin.server.Extension;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.UI;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.RedirectServerRpc;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

/**
 * Allows to define idle timeout warning shown on client side before session expires because of long idle period
 */
public class IdleAlarm extends AbstractExtension {

    public static final String DEFAULT_FORMATTING = "Your session will expire in less than "
            + IdleAlarmFormatting.SECS_TO_TIMEOUT + " seconds. Please click anywhere to extend session.";

    private Collection<RedirectListener> redirectListeners = new ArrayList<>();

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

        // For notifying server-side when redirect happened
        registerRpc(new RedirectServerRpc() {
            @Override
            public void redirected() {
                redirectListeners.forEach(RedirectListener::redirected);
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

    /**
     * Show IdleAlarmFormatting.SECS_TO_TIMEOUT with live seconds counting dow to 0.
     *
     * @param enabled
     * @return
     */
    public IdleAlarm setLiveTimeoutSecondsEnabled(boolean enabled) {
        getState().liveTimeoutSecondsEnabled = enabled;
        return this;
    }

    /**
     * @see #setLiveTimeoutSecondsEnabled(boolean)
     *
     * @return
     */
    public boolean isLiveTimeoutSecondsEnabled() {
        return getState().liveTimeoutSecondsEnabled;
    }

    /**
     * URL where to redirect when timeout happens, if timeoutRedirectURL == null, then do not redirect
     *
     * @param timeoutRedirectURL
     * @return
     */
    public IdleAlarm setTimeoutRedirectURL(String timeoutRedirectURL) {
        getState().timeoutRedirectURL = timeoutRedirectURL;
        return this;
    }

    /**
     * @see #setTimeoutRedirectURL(String)
     *
     * @return
     */
    public String getTimeoutRedirectURL() {
        return getState().timeoutRedirectURL;
    }

    /**
     * Shows/hides button for closing notification and resetting timer
     *
     * @param closeButtonEnabled
     * @return
     */
    public IdleAlarm setCloseButtonEnabled(boolean closeButtonEnabled) {
        getState().closeButtonEnabled = closeButtonEnabled;
        return this;
    }

    /**
     * @see #setCloseButtonEnabled(boolean)
     *
     * @return
     */
    public boolean isCloseButtonEnabled() {
        return getState().closeButtonEnabled;
    }

    /**
     * Show/hide button for immediately redirecting into URL given in #setTimeoutRedirectURL
     *
     * @param redirectButtonEnabled
     * @return
     */
    public IdleAlarm setRedirectButtonEnabled(boolean redirectButtonEnabled) {
        getState().redirectButtonEnabled = redirectButtonEnabled;
        return this;
    }

    /**
     * @see #setRedirectButtonEnabled(boolean)
     *
     * @return
     */
    public boolean isRedirectButtonEnabled() {
        return getState().redirectButtonEnabled;
    }

    /**
     * Set caption for close button
     *
     * @param closeButtonCaption
     * @return
     */
    public IdleAlarm setCloseButtonCaption(String closeButtonCaption) {
        getState().closeButtonCaption = closeButtonCaption;
        return this;
    }

    /**
     * @see #setCloseButtonCaption(String)
     *
     * @return
     */
    public String getCloseButtonCaption() {
        return getState().closeButtonCaption;
    }

    /**
     * Set caption for redirect button
     *
     * @param redirectButtonCaption
     * @return
     */
    public IdleAlarm setRedirectButtonCaption(String redirectButtonCaption) {
        getState().redirectButtonCaption = redirectButtonCaption;
        return this;
    }

    /**
     * @see #setRedirectButtonCaption(String)
     *
     * @return
     */
    public String getRedirectButtonCaption() {
        return getState().redirectButtonCaption;
    }

    /**
     * Add listener for redirect events
     *
     * @param redirectListener
     * @return
     */
    public IdleAlarm addRedirectListener(RedirectListener redirectListener) {
        redirectListeners.add(redirectListener);
        return this;
    }

    /**
     * @see #addRedirectListener(RedirectListener)
     *
     * @param redirectListener
     * @return
     */
    public IdleAlarm removeRedirectListener(RedirectListener redirectListener) {
        redirectListeners.remove(redirectListener);
        return this;
    }

    /**
     * Listener for redirect events
     */
    public interface RedirectListener {

        /**
         * Redirect happened
         */
        void redirected();
    }

}
