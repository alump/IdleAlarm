package org.vaadin.alump.idlealarm;

import com.vaadin.util.ReflectTools;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by alump on 7/8/17.
 */
@FunctionalInterface
public interface IdleClickListener extends Serializable {
    Method IDLE_CLICK_METHOD = ReflectTools.findMethod(IdleClickListener.class, "idleClick",
            new Class[]{IdleClickEvent.class});

    /**
     * Called when application defined button has been clicked
     * @param event Click event
     */
    void buttonClick(IdleClickEvent event);
}
