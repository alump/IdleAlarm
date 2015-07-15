package org.vaadin.alump.idlealarm;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;
import org.vaadin.alump.idlealarm.client.shared.IdleCountdownLabelState;
import org.vaadin.alump.idlealarm.client.shared.ResetTimeoutServerRpc;

import javax.swing.text.AbstractDocument;

/**
 * This class is more for debug use cases. It will shown time to idle timeout on client side
 */
public class IdleCountdownLabel extends AbstractComponent {


    public static final String DEFAULT_FORMATTING = IdleAlarmFormatting.SECS_TO_TIMEOUT
            + "s to idle timeout (" + IdleAlarmFormatting.SECS_MAX_IDLE_TIMEOUT + "s). "
            + IdleAlarmFormatting.SECS_SINCE_RESET + "s since last timeout reset.";

    /**
     * Creates new idle timeout label with default formatting and content mode
     */
    public IdleCountdownLabel() {
        this(DEFAULT_FORMATTING);
    }

    /**
     * Creates new IdleCountdownLabel with given text formatting
     * @param formatting Formatting of text shown, check IdleAlarmFormatting for template variables
     */
    public IdleCountdownLabel(String formatting) {
        this(formatting, ContentMode.TEXT);
    }

    /**
     * Creates new IdleCountdownLabel with given formatting and content mode
     * @param formatting Formatting of text shown, check IdleAlarmFormatting for template variables
     * @param contentMode Content mode used
     */
    public IdleCountdownLabel(String formatting, ContentMode contentMode) {
        setFormatting(formatting);
        setContentMode(contentMode);

        // Register dummy implementation to allow reset timeout calls
        registerRpc(new ResetTimeoutServerRpc() {
            @Override
            public void resetIdleTimeout() {
                //ignored, call is just to reset timeout
            }
        });
    }

    @Override
    protected IdleCountdownLabelState getState() {
        return (IdleCountdownLabelState) super.getState();
    }

    @Override
    public void attach() {
        super.attach();

        getState().maxInactiveInterval = IdleTimeoutServerUtil.resolveMaxInactiveInterval(getUI());
    }

    public void setFormatting(String formatting) {
        if(formatting == null) {
            throw new IllegalArgumentException("Formatting can not be null");
        }
        getState().formatting = formatting;
    }

    public void setContentMode(ContentMode contentMode) {
        if(contentMode == null) {
            throw new IllegalArgumentException("Content mode can not be null");
        }
        getState().contentMode = contentMode;
    }
}
