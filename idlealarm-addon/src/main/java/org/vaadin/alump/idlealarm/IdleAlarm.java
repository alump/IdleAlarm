package org.vaadin.alump.idlealarm;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.server.AbstractExtension;
import com.vaadin.server.Extension;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.ContentMode;
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

    private Map<Integer,IdleClickListener> buttonListeners = new HashMap<>();
    private AtomicInteger buttonCounter = new AtomicInteger(0);
    //private AtomicInteger resourceCounter = new AtomicInteger(0);

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
        registerRpc(new IdleAlarmServerRpc() {
            @Override
            public void resetIdleTimeout() {
                //ignored, call is just to reset session timeouts
            }

            @Override
            public void buttonClicked(int id, MouseEventDetails details) {
                Optional.ofNullable(buttonListeners.get(id)).ifPresent(listener -> {
                    IdleClickEvent event = new IdleClickEvent(IdleAlarm.this, details, id);
                    listener.buttonClick(event);
                });
            }
        });
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);
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

    /**
     * Get content mode of message in notification
     * @return Content mode
     */
    public ContentMode getContentMode() {
        return getState(false).contentMode;
    }

    /**
     * Set content mode of message in notification
     * @param contentMode Content mode of message
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm setContentMode(ContentMode contentMode) {
        getState().contentMode = Objects.requireNonNull(contentMode);
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
    public IdleAlarm setCountdown(boolean enabled) {
        getState().countdownTimeout = enabled;
        return this;
    }

    /**
     * @see #setCountdown(boolean)
     *
     * @return true if live
     */
    public boolean isCountdown() {
        return getState(false).countdownTimeout;
    }

    /**
     * URL where to redirect when timeout happens. Will set timeout action to REDIRECT
     *
     * @param url URL where browser will be redirected when timeout.
     * @return This IdleAlarm to allow command chaining
     * @see #setTimeoutAction(TimeoutAction)
     */
    public IdleAlarm setRedirectURL(String url) {
        getState().timeoutRedirectURL = Objects.requireNonNull(url);
        setTimeoutAction(TimeoutAction.REDIRECT);
        return this;
    }

    /**
     * Get redirect URL of timeout action
     * @see #setRedirectURL(String)
     *
     * @return This IdleAlarm to allow command chaining
     */
    public String getRedirectURL() {
        return getState(false).timeoutRedirectURL;
    }

    /**
     * Shows/hides button for closing notification and resetting timer. Notice that notification can be also closed by
     * clicking outside of it (or also inside of it when no buttons). This just allows to have clear close button for
     * users that might help with UX.
     *
     * @param closeButtonEnabled true to have separate close button, false to not have it
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm setCloseButtonEnabled(boolean closeButtonEnabled) {
        getState().closeEnabled = closeButtonEnabled;
        return this;
    }

    /**
     * @see #setCloseButtonEnabled(boolean)
     *
     * @return This IdleAlarm to allow command chaining
     */
    public boolean isCloseButtonEnabled() {
        return getState(false).closeEnabled;
    }

    /**
     * Remove all application specific buttons added with addButton
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm removeButtons() {
        getState().buttons.clear();
        return this;
    }

    /**
     * Add application specific button to warning notification
     * @param caption Caption of button added
     * @param listener Listener called when user clicks the button
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addButton(String caption, IdleClickListener listener) {
        return addButton(caption, Collections.EMPTY_LIST, listener);
    }

    /**
     * Add application specific button to warning notification
     * @param caption Caption of button added
     * @param styleNames Stylenames applied to this button (null can be used if no stylenames required)
     * @param listener Listener called when user clicks the button
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addButton(String caption, Collection<String> styleNames, IdleClickListener listener) {
        IdleAlarmState.ButtonState buttonState = new IdleAlarmState.ButtonState();
        buttonState.caption = caption;
        buttonState.styleNames = styleNames == null ? Collections.EMPTY_LIST : new ArrayList<>(styleNames);

        int buttonId = buttonCounter.incrementAndGet();
        buttonListeners.put(buttonId, Objects.requireNonNull(listener));

        getState().buttons.put(buttonId, buttonState);

        return this;
    }

    /**
     * Helper method to add redirecting button to notification. Will call addButton internally.
     * @param caption Caption of redirect button
     * @param url URL where browser window will be redirected
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addRedirectButton(String caption, String url) {
        return addRedirectButton(caption, Arrays.asList("redirect"), url);
    }

    /**
     * Helper method to add redirecting button to notification. Will call addButton internally.
     * @param caption Caption of redirect button
     * @param styleNames Stylename applied to button
     * @param url URL where browser window will be redirected
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addRedirectButton(String caption, Collection<String> styleNames, String url) {
        return addButton(caption, styleNames, event -> Page.getCurrent().open(url, null));
    }

    /**
     * Adds refresh button to notification. Will internally call addButton.
     * @param caption Caption of refresh button
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addRefreshButton(String caption) {
        return addRefreshButton(caption,  Arrays.asList("refresh"));
    }

    /**
     * Adds refresh button to notification. Will internally call addButton.
     * @param caption Caption of refresh button
     * @param styleNames Stylenames applied to button
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addRefreshButton(String caption, Collection<String> styleNames) {
        return addButton(caption, styleNames, event -> Page.getCurrent().reload());
    }

    /**
     * Set caption for close button
     *
     * @param caption Caption of close button
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm setCloseButtonCaption(String caption) {
        getState().closeCaption = caption;
        return this;
    }

    /**
     * @see #setCloseButtonCaption(String)
     *
     * @return This IdleAlarm to allow command chaining
     */
    public String getCloseButtonCaption() {
        return getState(false).closeCaption;
    }

    /**
     * Set timeout action performed automatically when session timeouts
     * @param action Action performed automatically at timeout
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm setTimeoutAction(TimeoutAction action) {
        getState().timeoutAction = Objects.requireNonNull(action);
        return this;
    }

    /**
     * Get current automatic timeout action
     * @return This IdleAlarm to allow command chaining
     */
    public TimeoutAction getTimeoutAction() {
        return getState(false).timeoutAction;
    }

    /**
     * Add stylename applied to timeout warning notification
     * @param styleName Stylename added
     * @return This IdleAlarm to allow command chaining
     */
    public IdleAlarm addStyleName(String styleName) {
        if(!getState().styleNames.contains(Objects.requireNonNull(styleName))) {
            getState().styleNames.add(styleName);
        }
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
