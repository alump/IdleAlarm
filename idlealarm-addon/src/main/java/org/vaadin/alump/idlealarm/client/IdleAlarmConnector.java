package org.vaadin.alump.idlealarm.client;

import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.shared.ui.Connect;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.RedirectServerRpc;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;
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

                if(!getState().refreshEnabled && !getState().redirectEnabled && !getState().closeEnabled) {
                    overlay.addStyleName("no-buttons");
                }

                getState().styleNames.forEach(stylename -> overlay.addStyleName(stylename));

                overlayLabel = new HTML();
                overlayLabel.addStyleName("idle-alarm-message");
                overlayContent.add(overlayLabel);

                if (getState().closeEnabled) {
                    overlayContent.add(createCloseButton());
                }

                if (getState().redirectEnabled && hasRedirectUrl) {
                    overlayContent.add(createRedirectButton());
                }

                if (getState().refreshEnabled) {
                    overlayContent.add(createRefreshButton());
                }

                // Use UI as owner
                overlay.setOwner(getConnection().getUIConnector().getWidget());
                overlay.addCloseHandler(new CloseHandler<PopupPanel>() {
                    @Override
                    public void onClose(CloseEvent<PopupPanel> event) {
                        if(event.isAutoClosed()) {
                            resetTimeout();
                        }
                    }
                });
            }

            String message = IdleAlarmMessageUtil.format(getState().message, event);
            IdleAlarmMessageUtil.setMessageToHtml(message, getState().contentMode, overlayLabel);

            if(!overlay.isShowing()) {
                overlay.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
                    @Override
                    public void setPosition(int offsetWidth, int offsetHeight) {
                        int windowWidth = Window.getClientWidth();
                        overlay.setPopupPosition((windowWidth - offsetWidth) / 2, 0);
                    }
                });
            }

            if (getState().liveTimeoutSecondsEnabled) {
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
        closeButton.setText(getState().closeCaption);
        closeButton.addStyleName("close-button");
        closeButton.addClickHandler(e -> {
            closeOverlay();
            resetTimeout();
        });
        return closeButton;
    }

    private Widget createRedirectButton() {
        VButton redirectButton = new VButton();
        redirectButton.setText(getState().redirectCaption);
        redirectButton.addStyleName("redirect-button");
        redirectButton.addClickHandler(e -> performTimeoutAction(TimeoutAction.REDIRECT));
        return redirectButton;
    }

    private Widget createRefreshButton() {
        VButton refreshButton = new VButton();
        refreshButton.setText(getState().refreshCaption);
        refreshButton.addStyleName("refresh-button");
        refreshButton.addClickHandler(e -> performTimeoutAction(TimeoutAction.REFRESH));
        return refreshButton;
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
                performTimeoutAction(null);
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
        getRpcProxy(ResetTimeoutServerRpc.class).resetIdleTimeout();
    }

    private boolean isOverlayShowing() {
        return overlay != null && overlay.isShowing();
    }

    private void performTimeoutAction(TimeoutAction manualAction) {
        if(manualAction == null && getState().timeoutAction == TimeoutAction.DEFAULT) {
            return;
        }

        final TimeoutAction action = getState().timeoutAction;
        final String url = getState().timeoutRedirectURL;

        if (manualAction != null) {
            // only for a manual redirect, in case of an automatic redirect UI is already closed
            getRpcProxy(RedirectServerRpc.class).redirected();

            Scheduler.get().scheduleFixedDelay(() -> {
                if(manualAction == TimeoutAction.REDIRECT) {
                    Window.Location.assign(url);
                } else {
                    Window.Location.reload();
                }
                return false;
            }, 200);
        } else {
            if(action == TimeoutAction.REDIRECT) {
                Window.Location.assign(url);
            } else {
                Window.Location.reload();
            }
        }
    }
}
