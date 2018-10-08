package org.vaadin.alump.idlealarm.client.shared;

/**
 * Action performed at timeout
 */
public enum TimeoutAction {
    /**
     * Default Vaadin action only
     */
    DEFAULT,
    /**
     * Refresh current URL
     */
    REFRESH,
    /**
     * Redirect to URL given by setRedirectURL method
     */
    REDIRECT;
}
