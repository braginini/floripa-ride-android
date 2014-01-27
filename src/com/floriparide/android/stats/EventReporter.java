package com.floriparide.android.stats;

import android.content.Context;

/**
 * Created by Mikhail Bragin
 */
public interface EventReporter {

	public void sendEvent(String category, String action, String event, Long value, Context context);
}
