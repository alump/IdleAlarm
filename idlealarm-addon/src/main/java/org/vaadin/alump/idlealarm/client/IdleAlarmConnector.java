package org.vaadin.alump.idlealarm.client;

import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.shared.ui.Connect;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

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
    private Button closeButton;
    private Button redirectButton;

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
        if(timeoutUtil != null) {
            timeoutUtil.stop();
            timeoutUtil = null;
        }
        super.onUnregister();
    }

    @Override
    public void onIdleTimeoutUpdate(IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        if (event.getSecondsToTimeout() <= getState().secondsBefore && event.getSecondsToTimeout() > 0) {
            if(overlay == null) {
                FocusPanel clickableContainer = new FocusPanel();
                FlowPanel overlayContent = new FlowPanel();

                overlay = new VOverlay();
                overlay.setAutoHideEnabled(true);
                overlay.addStyleName("idle-alarm-popup");
                overlay.add(clickableContainer);

                clickableContainer.add(overlayContent);
                clickableContainer.addStyleName("clickable-container");
                clickableContainer.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        resetTimeout();
                        closeOverlay();
                    }
                });

                overlayLabel = new HTML();
                overlayLabel.addStyleName("idle-alarm-message");
                overlayContent.add(overlayLabel);

                if (getState().closeButtonEnabled) {
                    createCloseButton();
                    overlayContent.add(closeButton);
                }
                if (getState().redirectButtonEnabled && hasRedirectURL()) {
                    createRedirectButton();
                    overlayContent.add(redirectButton);
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

            if (getState().liveTimeoutSecondsEnabled) {
                scheduleLiveSecondsToTimeoutUpdater(event);
            }
            if (hasRedirectURL()) {
                scheduleTimeoutRedirect(event.getSecondsToTimeout()*1000);
            }

        } else if(overlay != null) {
            closeOverlay();
        }
    }

    private void createCloseButton() {
        closeButton = new Button(getState().closeButtonCaption);
        closeButton.addStyleName("close-button");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeOverlay();
                resetTimeout();
            }
        });
    }

    private void createRedirectButton() {
        redirectButton = new Button(getState().redirectButtonCaption);
        redirectButton.addStyleName("redirect-button");
        redirectButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.Location.replace(getState().timeoutRedirectURL);
            }
        });
    }

    private void scheduleLiveSecondsToTimeoutUpdater(IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                int secondsToTimeout = timeoutUtil.secondsToIdleTimeout(IdleTimeoutClientUtil.getUnixTimeStamp());
                if (secondsToTimeout >= 0 && isOverlayShowing()) {
                    String msg = IdleAlarmMessageUtil.format(getState().message, secondsToTimeout, event.getSecondsSinceReset(), event.getMaxInactiveInterval());
                    IdleAlarmMessageUtil.setMessageToHtml(msg, getState().contentMode, overlayLabel);
                    return true;
                }
                return false;
            }
        }, 1000);
    }

    private void scheduleTimeoutRedirect(int timeoutMs) {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (isOverlayShowing()) {
                    Window.Location.replace(getState().timeoutRedirectURL);
                }
                return false;
            }
        }, timeoutMs);
    }

    private void closeOverlay() {
        if(overlay != null) {
            // Hide non-autoclosed to prevent timeout reset
            overlay.hide(false);
            overlay.removeFromParent();
            overlay = null;
        }
    }

    protected IdleTimeoutClientUtil getTimeoutUtil() {
        if(timeoutUtil == null) {
            timeoutUtil = new IdleTimeoutClientUtil(this, this);
        }
        return timeoutUtil;
    }

    protected void resetTimeout() {
        getRpcProxy(ResetTimeoutServerRpc.class).resetIdleTimeout();
    }

    private boolean hasRedirectURL() {
        return getState().timeoutRedirectURL!=null && getState().timeoutRedirectURL.length()>3;
    }

    private boolean isOverlayShowing() {
        return overlay != null && overlay.isShowing();
    }
}
