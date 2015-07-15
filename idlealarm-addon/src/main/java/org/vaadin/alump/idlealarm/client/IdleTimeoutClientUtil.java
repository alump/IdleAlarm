package org.vaadin.alump.idlealarm.client;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ui.AbstractConnector;

import java.sql.Time;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Util class that attaches to application connection
 */
public class IdleTimeoutClientUtil {

    private final static Logger LOGGER = Logger.getLogger(IdleTimeoutClientUtil.class.getName());

    private boolean running = false;

    private HandlerRegistration communicationReg = null;

    private IdleTimeoutListener listener = null;

    private Integer lastRequest = null;

    private TimeoutTimer timer;

    private Integer callWhenSecondsLeft = null;

    private int maxInactiveInterval;

    public final static int DEFAULT_CALL_FREQUENCY_MS = 1000;

    public static class IdleTimeoutUpdateEvent {
        private final int secondsSinceReset;
        private final int secondsToTimeout;
        private final int maxInactiveInterval;

        public IdleTimeoutUpdateEvent(int secondsSinceReset, int secondsToTimeout, int maxInactiveInterval) {
            this.secondsSinceReset = secondsSinceReset;
            this.secondsToTimeout = secondsToTimeout;
            this.maxInactiveInterval = maxInactiveInterval;
        }

        public int getSecondsSinceReset() {
            return secondsSinceReset;
        }

        public int getSecondsToTimeout() {
            return secondsToTimeout;
        }

        public int getMaxInactiveInterval() {
            return maxInactiveInterval;
        }
    }

    public interface IdleTimeoutListener {
        void onIdleTimeoutUpdate(IdleTimeoutUpdateEvent event);
    }

    protected class TimeoutTimer extends Timer {

        public void runAndScheduleNext() {
            run();
            cancel();

            if(lastRequest == null) {
                throw new IllegalStateException("Invalid state, no last request");
            }

            if(callWhenSecondsLeft != null) {
                int nextTimeout = lastRequest.intValue() + maxInactiveInterval;
                int secondsLeft = nextTimeout - getUnixTimeStamp() - callWhenSecondsLeft.intValue();
                if(secondsLeft <= 0) {
                    run();
                } else {
                    schedule(secondsLeft * 1000);
                }
            } else {
                schedule(DEFAULT_CALL_FREQUENCY_MS);
            }
        }

        @Override
        public void run() {
            int timestamp = getUnixTimeStamp();
            int toNextTimeout = secondsToIdleTimeout(timestamp);
            if(toNextTimeout < 0) {
                toNextTimeout = 0;
            }
            int sinceLastUpdate = secondsSinceLastUpdate(timestamp);
            IdleTimeoutUpdateEvent event = new IdleTimeoutUpdateEvent(sinceLastUpdate, toNextTimeout,
                    maxInactiveInterval);
            listener.onIdleTimeoutUpdate(event);
            if(callWhenSecondsLeft == null && toNextTimeout > 0) {
                schedule(DEFAULT_CALL_FREQUENCY_MS);
            }
        }
    }

    public IdleTimeoutClientUtil(AbstractConnector connector, IdleTimeoutListener listener) {
        this.listener = listener;
        communicationReg = connector.getConnection().addHandler(ApplicationConnection.RequestStartingEvent.TYPE,
                new ApplicationConnection.CommunicationHandler() {

            @Override
            public void onRequestStarting(ApplicationConnection.RequestStartingEvent e) {
                if(running) {
                    lastRequest = getUnixTimeStamp();
                    getTimer().runAndScheduleNext();
                }
            }

            @Override
            public void onResponseHandlingStarted(ApplicationConnection.ResponseHandlingStartedEvent e) {
                //ignored
            }

            @Override
            public void onResponseHandlingEnded(ApplicationConnection.ResponseHandlingEndedEvent e) {
                //ignored
            }
        });
    }

    public static int getUnixTimeStamp() {
        Date date = new Date();
        return (int) (date.getTime() * .001);
    }

    protected int secondsSinceLastUpdate(int timestamp) {
        if(lastRequest == null) {
            throw new IllegalStateException("No last request");
        }
        return timestamp - lastRequest;
    }

    protected int secondsToIdleTimeout(int timestamp) {
        if(lastRequest == null) {
            throw new IllegalStateException("No last request");
        }
        return lastRequest.intValue() + maxInactiveInterval - timestamp;
    }

    protected TimeoutTimer getTimer() {
        if(timer != null) {
            return timer;
        }
        timer = new TimeoutTimer();
        return timer;
    }

    /**
     * Start timer that will call regularly the listener. Start can be only called once.
     * @param maxInactiveInterval Idle timeout used to calculate seconds left
     */
    public void start(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
        running = true;
    }

    /**
     * Start timer that will call listener when given amount of seconds is left. Start can be only called once.
     * @param maxInactiveInterval Idle timeout used to calculate seconds left
     * @param callWhenSecondsLeft Listener will be called when there is less or equal amount of seconds left to timeout
     */
    public void start(int maxInactiveInterval, int callWhenSecondsLeft) {
        this.callWhenSecondsLeft = callWhenSecondsLeft;
        start(maxInactiveInterval);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * This method must be called to release resources
     */
    public void stop() {
        running = false;
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        if (communicationReg != null) {
            communicationReg.removeHandler();
            communicationReg = null;
        }
    }

}
