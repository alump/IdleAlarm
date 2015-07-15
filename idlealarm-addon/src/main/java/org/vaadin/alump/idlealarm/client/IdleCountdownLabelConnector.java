package org.vaadin.alump.idlealarm.client;

import com.google.gwt.user.client.ui.HTML;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import org.vaadin.alump.idlealarm.client.shared.IdleCountdownLabelState;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

import java.util.logging.Logger;

/**
 * Connector for IdleCountdownLabel
 */
@Connect(value = org.vaadin.alump.idlealarm.IdleCountdownLabel.class, loadStyle = Connect.LoadStyle.LAZY)
public class IdleCountdownLabelConnector extends AbstractComponentConnector implements
        IdleTimeoutClientUtil.IdleTimeoutListener {

    private final static Logger LOGGER = Logger.getLogger(IdleCountdownLabelConnector.class.getName());

    protected IdleTimeoutClientUtil timeoutUtil = null;

    @Override
    protected void init() {
        super.init();
        getWidget().setHTML("&nbsp;");
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
    public HTML getWidget() {
        return (HTML)super.getWidget();
    }

    @Override
    public IdleCountdownLabelState getState() {
        return (IdleCountdownLabelState)super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent event) {
        super.onStateChanged(event);

        if(this.getConnection() == null) {
            LOGGER.severe("No connection!");
        } else if(!getTimeoutUtil().isRunning()) {
            getTimeoutUtil().start(getState().maxInactiveInterval);
            getRpcProxy(ResetTimeoutServerRpc.class).resetIdleTimeout();
        }
    }

    @Override
    public void onIdleTimeoutUpdate(IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        String message = IdleAlarmMessageUtil.format(getState().formatting, event);
        IdleAlarmMessageUtil.setMessageToHtml(message, getState().contentMode, getWidget());
    }

    protected IdleTimeoutClientUtil getTimeoutUtil() {
        if(timeoutUtil == null) {
            timeoutUtil = new IdleTimeoutClientUtil(this, this);
        }
        return timeoutUtil;
    }
}
