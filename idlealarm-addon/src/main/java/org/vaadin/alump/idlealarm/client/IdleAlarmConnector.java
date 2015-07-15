package org.vaadin.alump.idlealarm.client;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.shared.ui.Connect;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmState;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

import java.util.logging.Logger;

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
                overlay = new VOverlay();
                overlay.setAutoHideEnabled(true);
                overlay.addStyleName("idle-alarm-popup");
                overlayLabel = new HTML();
                overlayLabel.addStyleName("idle-alarm-message");
                overlay.add(overlayLabel);
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
        } else if(overlay != null) {
            closeOverlay();
        }
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
}
