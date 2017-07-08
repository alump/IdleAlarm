package org.vaadin.alump.idlealarm.client;

import java.util.Collection;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.vaadin.client.MouseEventDetailsBuilder;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.Connect;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmServerRpc;
import org.vaadin.alump.idlealarm.client.shared.TimeoutAction;

/**
 * IdleAlarm connector presenting warnings before idle timeout happens
 */
@Connect(org.vaadin.alump.idlealarm.IdleAlarm.class)
public class IdleAlarmConnector extends AbstractExtensionConnector
        implements IdleTimeoutClientUtil.IdleTimeoutListener {

    private final static Logger LOGGER = Logger.getLogger(IdleAlarmConnector.class.getName());

    protected IdleTimeoutClientUtil timeoutUtil = null;

    private VOverlay overlay;
    private HTML overlayLabel;
    private Timer actionTimer;

    @Override
    public IdleAlarmState getState() {
        return (IdleAlarmState)super.getState();
    }

    @Override
    protected void extend(ServerConnector target) {
        //ignore
    }

    @Override
    public void onStateChanged(StateChangeEvent event) {
        super.onStateChanged(event);

        if(this.getConnection() == null) {
            LOGGER.severe("No connection!");
        } else if(!getTimeoutUtil().isRunning()) {
            getTimeoutUtil().start(getState().maxInactiveInterval, getState().secondsBefore);
            resetTimeout();
        }
    }

    @Override
    public void onUnregister() {
        if(actionTimer != null) {
            actionTimer.cancel();
        }
        if(timeoutUtil != null) {
            timeoutUtil.stop();
            timeoutUtil = null;
        }
        super.onUnregister();
    }

    @Override
    public void onIdleTimeoutUpdate(IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        if (event.getSecondsToTimeout() <= getState().secondsBefore && event.getSecondsToTimeout() > 0) {
            boolean hasRedirectUrl = getState().timeoutRedirectURL != null && !getState().timeoutRedirectURL.isEmpty();

            if(overlay == null) {
                FlowPanel overlayContent = new FlowPanel();

                overlay = new VOverlay();
                overlay.add(overlayContent);
                overlay.setAutoHideEnabled(true);
                overlay.addStyleName("idle-alarm-popup");

                if(!getState().closeEnabled && getState().buttons.size() == 0) {
                    overlay.addStyleName("no-buttons");
                }

                getState().styleNames.forEach(stylename -> overlay.addStyleName(stylename));

                overlayLabel = new HTML();
                overlayLabel.addStyleName("idle-alarm-message");
                overlayContent.add(overlayLabel);

                if (getState().closeEnabled) {
                    overlayContent.add(createCloseButton());
                    overlay.addStyleName("with-close");
                }

                int buttonIndex = 0;
                for (Integer id : getState().buttons.keySet()) {
                    ++buttonIndex;
                    IdleAlarmState.ButtonState buttonState = getState().buttons.get(id);
                    overlayContent.add(createButton(id, buttonIndex, buttonState.caption, buttonState.styleNames));
                }

                // Use UI as owner
                overlay.setOwner(getConnection().getUIConnector().getWidget());
                overlay.addCloseHandler(e -> {
                    if(e.isAutoClosed()) {
                        resetTimeout();
                    }
                });
            }

            String message = IdleAlarmMessageUtil.format(getState().message, event);
            IdleAlarmMessageUtil.setMessageToHtml(message, getState().contentMode, overlayLabel);

            if(!overlay.isShowing()) {
                overlay.setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
                    int windowWidth = Window.getClientWidth();
                    overlay.setPopupPosition((windowWidth - offsetWidth) / 2, 0);
                });
            }

            if (getState().countdownTimeout) {
                scheduleLiveSecondsToTimeoutUpdater(event);
            }
            if (getState().timeoutAction != TimeoutAction.REDIRECT) {
                scheduleTimeoutAction(event.getSecondsToTimeout()*1000);
            }

        } else if(overlay != null) {
            closeOverlay();
        }
    }

    private Widget createCloseButton() {
        VButton closeButton = new VButton();
        if(getState().closeCaption != null) {
            closeButton.setText(getState().closeCaption);
        } else {
            closeButton.setHtml("&#10005;");
        }
        closeButton.addStyleName("close-button");
        closeButton.addClickHandler(e -> {
            closeOverlay();
            resetTimeout();
        });
        return closeButton;
    }

    private Widget createButton(final int id, int index, String caption, Collection<String> styleNames) {
        VButton redirectButton = new VButton();
        if(caption != null) {
            redirectButton.setText(caption);
        }
        redirectButton.addStyleName("button-" + index);
        styleNames.forEach(styleName -> redirectButton.addStyleName(styleName));
        redirectButton.addClickHandler(e -> {
            MouseEventDetails details = MouseEventDetailsBuilder.buildMouseEventDetails(e.getNativeEvent());
            getRpcProxy(IdleAlarmServerRpc.class).buttonClicked(id, details);
        });
        return redirectButton;
    }

    private void scheduleLiveSecondsToTimeoutUpdater(IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        Scheduler.get().scheduleFixedPeriod(() -> {
            if(timeoutUtil ==  null) {
                return false;
            }
            int secondsToTimeout = timeoutUtil.secondsToIdleTimeout(IdleTimeoutClientUtil.getUnixTimeStamp());
            if (secondsToTimeout >= 0 && isOverlayShowing()) {
                String msg = IdleAlarmMessageUtil.format(getState().message, secondsToTimeout,
                        event.getSecondsSinceReset(), event.getMaxInactiveInterval());
                IdleAlarmMessageUtil.setMessageToHtml(msg, getState().contentMode, overlayLabel);
                return true;
            }
            return false;
        }, 1000);
    }

    private void scheduleTimeoutAction(int timeoutMs) {
        actionTimer = new Timer() {
            @Override
            public void run() {
                performTimeoutAction();
            }};
        actionTimer.schedule(timeoutMs);
    }

    private void closeOverlay() {
        if(overlay != null) {
            // Hide non-autoclosed to prevent timeout reset
            overlay.hide(false);
            overlay.removeFromParent();
            overlay = null;
        }
        if (actionTimer !=null) {
            actionTimer.cancel();
        }
    }

    protected IdleTimeoutClientUtil getTimeoutUtil() {
        if(timeoutUtil == null) {
            timeoutUtil = new IdleTimeoutClientUtil(this, this);
        }
        return timeoutUtil;
    }

    protected void resetTimeout() {
        getRpcProxy(IdleAlarmServerRpc.class).resetIdleTimeout();
    }

    private boolean isOverlayShowing() {
        return overlay != null && overlay.isShowing();
    }

    private void performTimeoutAction() {
        closeOverlay();

        if(getState().timeoutAction == TimeoutAction.DEFAULT) {
            return;
        }

        final TimeoutAction action = getState().timeoutAction;
        final String url = getState().timeoutRedirectURL;

        if(action == TimeoutAction.REDIRECT) {
            Window.Location.assign(url);
        } else {
            Window.Location.reload();
        }
    }
}
