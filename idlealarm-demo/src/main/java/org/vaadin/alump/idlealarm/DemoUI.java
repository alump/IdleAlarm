package org.vaadin.alump.idlealarm;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;
import org.vaadin.alump.idlealarm.client.shared.TimeoutAction;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import java.sql.Time;
import java.util.HashSet;
import java.util.Set;

/**
 * Demo UI of IdleAlarm addon
 */
@Theme("demo")
@Title("IdleAlarm Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI {

    private static final String STYLED_FORMATTING = "Lennät muuten ulos <b>"
            + IdleAlarmFormatting.SECS_TO_TIMEOUT + "</b> sekunnin kuluttua, ellet tee mitään.";

    private static final int IDLE_TIMEOUT_SECONDS = 60;

    private static final String GUIDE = "IdleAlarm add-on is designed to be used with Vaadin's idle timeout feature. "
            + "Add-on adds option to show alarm to user when sessions is about to expire because of long idle period.";

    private static final String NOTICE = "<b>Please notice</b> that there is always some extra delay (from seconds to "
        + "few minutes) before session really gets expired. As the idle time out in this application is set to very "
        + " short (60 seconds), you can easily see this extra time here.";

    private Set<Component> disabledComponents = new HashSet<>();

    // This add-on old works when closeIdleSessions init parameter is true
    @WebServlet(value = "/*", asyncSupported = true, initParams = {
            @WebInitParam(name="closeIdleSessions", value="true")
    })
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "org.vaadin.alump.idlealarm.DemoWidgetSet")
    public static class Servlet extends VaadinServlet implements SessionInitListener {

        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(this);
        }

        @Override
        public void sessionInit(SessionInitEvent event) throws ServiceException {
            event.getSession().getSession().setMaxInactiveInterval(IDLE_TIMEOUT_SECONDS);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setMargin(true);
        layout.setSpacing(true);
        setContent(layout);

        layout.addComponent(new Label(GUIDE));
        layout.addComponent(new Label(NOTICE, ContentMode.HTML));

        // -- Inputs to modify IdleAlarm --

        HorizontalLayout row = createRow(layout);
        row.setWidth(100, Unit.PERCENTAGE);
        row.setCaption("Setup warning message (values can be modified only in disabled state)");

        final TextField secondsBefore = new TextField("Seconds");
        secondsBefore.setValue("50");
        secondsBefore.setWidth(60, Unit.PIXELS);
        row.addComponent(secondsBefore);
        disabledComponents.add(secondsBefore);

        final TextArea warningMessage = new TextArea("Message");
        warningMessage.setValue(IdleAlarm.DEFAULT_FORMATTING);
        warningMessage.setWidth(100, Unit.PERCENTAGE);
        row.addComponent(warningMessage);
        row.setExpandRatio(warningMessage, 1f);
        disabledComponents.add(warningMessage);

        final ComboBox contentMode = new ComboBox("Content mode");
        contentMode.addItem(ContentMode.TEXT);
        contentMode.addItem(ContentMode.PREFORMATTED);
        contentMode.addItem(ContentMode.HTML);
        contentMode.setValue(ContentMode.TEXT);
        contentMode.setNullSelectionAllowed(false);
        row.addComponent(contentMode);
        disabledComponents.add(contentMode);

        final Button enableButton = new Button("Enable");
        row.addComponent(enableButton);
        row.setComponentAlignment(enableButton, Alignment.TOP_RIGHT);
        final Button disableButton = new Button("Disable");
        disableButton.setEnabled(false);
        row.addComponent(disableButton);
        row.setComponentAlignment(disableButton, Alignment.TOP_LEFT);

        final String START_BLOCK = "<span class=\"keyword\">";
        final String END_BLOCK = "</span>";
        Label keywords = new Label("Keywords you can use in message are " + START_BLOCK
                + IdleAlarmFormatting.SECS_TO_TIMEOUT + END_BLOCK + ", " + START_BLOCK
                + IdleAlarmFormatting.SECS_MAX_IDLE_TIMEOUT + END_BLOCK + " " + " and "
                + START_BLOCK + IdleAlarmFormatting.SECS_SINCE_RESET + END_BLOCK + ".",
                ContentMode.HTML);
        layout.addComponent(keywords);

        row = createRow(layout);
        CheckBox liveCountDownEnabled = new CheckBox("Live seconds timeout counter enabled");
        liveCountDownEnabled.setValue(false);
        CheckBox closeButtonEnabled = new CheckBox("Close button");
        CheckBox refreshButtonEnabled = new CheckBox("Refresh button");
        CheckBox redirectButtonEnabled = new CheckBox("Redirect button");
        row.addComponents(liveCountDownEnabled, closeButtonEnabled, refreshButtonEnabled, redirectButtonEnabled);
        disabledComponents.add(liveCountDownEnabled);
        disabledComponents.add(closeButtonEnabled);
        disabledComponents.add(refreshButtonEnabled);
        disabledComponents.add(redirectButtonEnabled);

        row = createRow(layout);
        row.setWidth(100, Unit.PERCENTAGE);
        ComboBox timeoutAction = new ComboBox("Timeout Action");
        timeoutAction.addItems(TimeoutAction.values());
        timeoutAction.setValue(TimeoutAction.DEFAULT);
        TextField timeoutURLField = new TextField("URL where to redirect after timeout");
        timeoutURLField.setValue("http://www.google.com");
        timeoutURLField.setWidth(100, Unit.PERCENTAGE);
       // timeoutURLField.setPlaceholder("Write URL here where browser will be redirected");
        row.addComponents(timeoutAction, timeoutURLField);
        row.setExpandRatio(timeoutURLField, 1f);
        disabledComponents.add(timeoutAction);
        disabledComponents.add(timeoutURLField);

        enableButton.addClickListener(event -> {

            if(redirectButtonEnabled.getValue() && timeoutURLField.isEmpty()) {
                Notification.show("Please provide redirect URL", Notification.Type.ERROR_MESSAGE);
                return;
            }

            disabledComponents.forEach(c -> c.setEnabled(false));
            enableButton.setEnabled(false);
            disableButton.setEnabled(true);

            IdleAlarm.get().setSecondsBefore(Integer.valueOf(secondsBefore.getValue()))
                    .setMessage(warningMessage.getValue())
                    .setContentMode((ContentMode) contentMode.getValue())
                    .setLiveTimeoutSecondsEnabled(liveCountDownEnabled.getValue())
                    .setCloseButtonEnabled(closeButtonEnabled.getValue())
                    .setRefreshButtonEnabled(refreshButtonEnabled.getValue())
                    .setTimeoutAction((TimeoutAction) timeoutAction.getValue())
                    .addStyleName(IdleAlarm.COMPACT_STYLING)
                    .setRedirectButtonEnabled(redirectButtonEnabled.getValue())
                    .setRedirectURL(timeoutURLField.getValue())
                    .addRedirectListener(()-> {
                        System.out.println("*** redirect happened ***");
                    });
        });

        disableButton.addClickListener(event -> {
            disabledComponents.forEach(c -> c.setEnabled(true));
            enableButton.setEnabled(true);
            disableButton.setEnabled(false);
            IdleAlarm.unload();
        });

        // -- Labels for debugging --

        row = createRow(layout);

        IdleCountdownLabel label = new IdleCountdownLabel();
        label.setCaption("IdleCountdownLabel (mainly for debugging):");
        row.addComponent(label);

        IdleCountdownLabel styledLabel = new IdleCountdownLabel(STYLED_FORMATTING);
        styledLabel.setContentMode(ContentMode.HTML);
        styledLabel.setCaption("IdleCountdownLabel (formatting & styling)");
        row.addComponent(styledLabel);

        Button resetTimeout = new Button("Reset timeout by calling server", event -> {
           Notification.show("Idle time reset");
        });
        layout.addComponent(resetTimeout);

    }

    private HorizontalLayout createRow(ComponentContainer parent) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        parent.addComponent(row);
        return row;
    }

}
