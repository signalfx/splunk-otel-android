package com.splunk.rum.incubating;

import java.util.function.Supplier;

/**
 * Experimental interface, used to return the name of the currently visible screen.
 */
public interface CurrentlyVisibleScreen extends Supplier<String> {

}
