package org.vaadin.alump.idlealarm;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import org.vaadin.alump.idlealarm.client.shared.IdleAlarmFormatting;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

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

        Label guide = new Label(GUIDE);
        guide.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(guide);
        Label notice = new Label(NOTICE, ContentMode.HTML);
        notice.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(notice);

        // -- Inputs to modify IdleAlarm --

        HorizontalLayout row = createRow(layout);
        row.setWidth(100, Unit.PERCENTAGE);
        row.setCaption("Setup warning message (values can be modified only in disabled state)");

        final TextField secondsBefore = new TextField("Seconds");
        secondsBefore.setValue("40");
        secondsBefore.setWidth(60, Unit.PIXELS);
        row.addComponent(secondsBefore);

        final TextArea warningMessage = new TextArea("Message");
        warningMessage.setValue(IdleAlarm.DEFAULT_FORMATTING);
        warningMessage.setWidth(100, Unit.PERCENTAGE);
        row.addComponent(warningMessage);
        row.setExpandRatio(warningMessage, 1f);

        final ComboBox contentMode = new ComboBox("Content mode");
        contentMode.setItems(ContentMode.TEXT, ContentMode.PREFORMATTED, ContentMode.HTML);
        contentMode.setValue(ContentMode.TEXT);
        contentMode.setEmptySelectionAllowed(false);
        row.addComponent(contentMode);

        final Button enableButton = new Button("Enable");
        row.addComponent(enableButton);
        row.setComponentAlignment(enableButton, Alignment.TOP_RIGHT);
        final Button disableButton = new Button("Disable");
        disableButton.setEnabled(false);
        row.addComponent(disableButton);
        row.setComponentAlignment(disableButton, Alignment.TOP_LEFT);

        enableButton.addClickListener(event -> {
            enableButton.setEnabled(false);
            disableButton.setEnabled(true);
            secondsBefore.setEnabled(false);
            warningMessage.setEnabled(false);
            contentMode.setEnabled(false);

            IdleAlarm.get().setSecondsBefore(Integer.valueOf(secondsBefore.getValue()))
                    .setMessage(warningMessage.getValue())
                    .setContentMode((ContentMode) contentMode.getValue());
        });

        disableButton.addClickListener(event -> {
            enableButton.setEnabled(true);
            disableButton.setEnabled(false);
            secondsBefore.setEnabled(true);
            warningMessage.setEnabled(true);
            contentMode.setEnabled(true);

            IdleAlarm.unload();
        });

        final String START_BLOCK = "<span class=\"keyword\">";
        final String END_BLOCK = "</span>";
        Label keywords = new Label("Keywords you can use in message are " + START_BLOCK
                + IdleAlarmFormatting.SECS_TO_TIMEOUT + END_BLOCK + ", " + START_BLOCK
                + IdleAlarmFormatting.SECS_MAX_IDLE_TIMEOUT + END_BLOCK + " " + " and "
                + START_BLOCK + IdleAlarmFormatting.SECS_SINCE_RESET + END_BLOCK + ".",
                ContentMode.HTML);
        layout.addComponent(keywords);

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

        Link gitHub = new Link("IdleAlarm in GitHub (source code, issue tracker...)",
                new ExternalResource("https://github.com/alump/IdleAlarm"));
        layout.addComponent(gitHub);

    }

    private HorizontalLayout createRow(ComponentContainer parent) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        parent.addComponent(row);
        return row;
    }

}
