package org.vaadin.alump.idlealarm.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.shared.ui.label.ContentMode;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;

/**
 * Created by alump on 15/07/15.
 */
public class IdleAlarmMessageUtil {

    public static String format(String formatting, IdleTimeoutClientUtil.IdleTimeoutUpdateEvent event) {
        String message = formatting;
        message = message.replaceAll(IdleAlarmFormatting.SECS_TO_TIMEOUT, "" + event.getSecondsToTimeout());
        message = message.replaceAll(IdleAlarmFormatting.SECS_SINCE_RESET, "" + event.getSecondsSinceReset());
        message = message.replaceAll(IdleAlarmFormatting.SECS_MAX_IDLE_TIMEOUT, "" + event.getMaxInactiveInterval());
        return message;
    }

    public static void setMessageToHtml(String message, ContentMode contentMode, HTML widget) {
        if(contentMode == ContentMode.HTML) {
            widget.setHTML(message);
        } else if(contentMode == ContentMode.PREFORMATTED) {
            PreElement preElement = Document.get().createPreElement();
            preElement.setInnerText(message);
            widget.setHTML(preElement.getString());
        } else {
            widget.setText(message);
        }
    }
}
