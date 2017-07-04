package org.vaadin.alump.idlealarm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.vaadin.server.AbstractExtension;
import com.vaadin.server.Extension;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.UI;
import org.vaadin.alump.idlealarm.client.shared.*;

/**
 * Allows to define idle timeout warning shown on client side before session expires because of long idle period
 */
public class IdleAlarm extends AbstractExtension {

    /**
     * Styling that layouts buttons nicely
     */
    public static final String COMPACT_STYLING = "compact-styling";

    public static final String DEFAULT_FORMATTING =
            "Your session will expire in less than "
            + IdleAlarmFormatting.SECS_TO_TIMEOUT
            + " seconds. Please click anywhere outside this notification to extend session.";

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
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);

        // Sanity check, URL has to be defined when redirect action or button are enabled
        if(getTimeoutAction() == TimeoutAction.REDIRECT || isRedirectButtonEnabled()) {
            if(getRedirectURL() == null || getRedirectURL().isEmpty()) {
                throw new IllegalStateException("Redirect action or button enabled, but redirect URL not defined!");
            }
        }
    }

    @Override
    protected IdleAlarmState getState() {
        return (IdleAlarmState)super.getState();
    }

    @Override
    protected IdleAlarmState getState(boolean markDirty) {
        return (IdleAlarmState)super.getState(markDirty);
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
        return getState(false).secondsBefore;
    }

    /**
     * Get value resolved from VaadinSession
     * @return Idle timeout in seconds
     */
    public int getMaxInactiveInternal() {
        return getState(false).maxInactiveInterval;
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
        return getState(false).contentMode;
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
        return getState(false).message;
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
        return getState(false).liveTimeoutSecondsEnabled;
    }

    /**
     * URL where to redirect when timeout happens. To set this automatic action, use
     * setTimeoutAction(TimeoutAction.REDIRECT) or enable redirect button with setRedirectButtonEnabled(true)
     *
     * @param url URL where browser will be redirected when timeout. Can not be null or empty if redirect action
     *            or button is used.
     * @return
     * @see #setTimeoutAction(TimeoutAction)
     */
    public IdleAlarm setRedirectURL(String url) {
        getState().timeoutRedirectURL = url;
        return this;
    }

    /**
     * Get timeout URL. Notice that given URL will be only used if action is redirect.
     * @see #setRedirectURL(String)
     *
     * @return
     */
    public String getRedirectURL() {
        return getState(false).timeoutRedirectURL;
    }

    /**
     * Shows/hides button for closing notification and resetting timer. You need to have timeout redirect URL defined
     * also to see the button.
     *
     * @param closeButtonEnabled
     * @return
     */
    public IdleAlarm setCloseButtonEnabled(boolean closeButtonEnabled) {
        getState().closeEnabled = closeButtonEnabled;
        return this;
    }

    /**
     * @see #setCloseButtonEnabled(boolean)
     *
     * @return
     */
    public boolean isCloseButtonEnabled() {
        return getState(false).closeEnabled;
    }

    /**
     * Show/hide button for immediately redirecting into URL given in #setRedirectURL. Notice that if redirect URl
     *
     * @param redirectButtonEnabled
     * @return
     */
    public IdleAlarm setRedirectButtonEnabled(boolean redirectButtonEnabled) {
        getState().redirectEnabled = redirectButtonEnabled;
        return this;
    }

    /**
     * @see #setRedirectButtonEnabled(boolean)
     *
     * @return
     */
    public boolean isRedirectButtonEnabled() {
        return getState(false).redirectEnabled;
    }

    /**
     * Show/hide button for immediately refreshing current URL
     *
     * @param enabled
     * @return
     */
    public IdleAlarm setRefreshButtonEnabled(boolean enabled) {
        getState().refreshEnabled = enabled;
        return this;
    }

    /**
     * @see #setRedirectButtonEnabled(boolean)
     *
     * @return
     */
    public boolean isRefreshButtonEnabled() {
        return getState(false).refreshEnabled;
    }

    /**
     * Set caption for close button
     *
     * @param caption
     * @return
     */
    public IdleAlarm setCloseButtonCaption(String caption) {
        getState().closeCaption = Objects.requireNonNull(caption);
        return this;
    }

    /**
     * @see #setCloseButtonCaption(String)
     *
     * @return
     */
    public String getCloseButtonCaption() {
        return getState(false).closeCaption;
    }

    /**
     * Set caption for redirect button
     *
     * @param caption
     * @return
     */
    public IdleAlarm setRedirectButtonCaption(String caption) {
        getState().redirectCaption = Objects.requireNonNull(caption);
        return this;
    }

    /**
     * @see #setRedirectButtonCaption(String)
     *
     * @return
     */
    public String getRedirectButtonCaption() {
        return getState(false).redirectCaption;
    }

    /**
     * Set caption for refresh button. Remember also to enable button with setRefreshButtonEnabled(boolean)
     *
     * @param caption
     * @return
     */
    public IdleAlarm setRefreshButtonCaption(String caption) {
        getState().refreshCaption = Objects.requireNonNull(caption);
        return this;
    }

    /**
     * @see #setRefreshButtonCaption(String)
     *
     * @return
     */
    public String getRefreshButtonCaption() {
        return getState(false).refreshCaption;
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
    public interface RedirectListener extends Serializable {

        /**
         * Redirect happened
         */
        void redirected();
    }

    /**
     * Set timeout action performed automatically when session timeouts
     * @param action Action performed automatically at timeout
     * @return
     */
    public IdleAlarm setTimeoutAction(TimeoutAction action) {
        getState().timeoutAction = Objects.requireNonNull(action);
        return this;
    }

    /**
     * Get current automatic timeout action
     * @return
     */
    public TimeoutAction getTimeoutAction() {
        return getState(false).timeoutAction;
    }

    /**
     * Add stylename applied to timeout warning notification
     * @param styleName Stylename added
     * @return
     */
    public IdleAlarm addStyleName(String styleName) {
        getState().styleNames.add(Objects.requireNonNull(styleName));
        return this;
    }

    /**
     * Remove stylename applied to timeout warning notification
     * @param styleName Stylename removed
     */
    public void removeStyleName(String styleName) {
        getState().styleNames.remove(styleName);
    }

}
