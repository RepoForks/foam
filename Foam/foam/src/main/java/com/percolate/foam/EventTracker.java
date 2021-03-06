package com.percolate.foam;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Event tracking class.  Registers a Application.ActivityLifecycleCallbacks that gives us
 * information about the Activity lifecycle as the application is being used.
 *
 * After {@link #start()} is called, all passed in {@link #services} classes will receive
 * a logEvent() request when new activities are viewed (on <code>onActivityResumed</code>).
 *
 * For details see {@see Application.ActivityLifecycleCallbacks} and {@see EventTrackingService}.
 */
class EventTracker {

    private Context context;

    Utils utils;

    /* Services that will receive tracking events */
    private List<EventTrackingService> services;

    /* Only send events over WiFi */
    private boolean wifiOnly;

    public EventTracker(Context context, List<EventTrackingService> services, boolean wifiOnly) {
        this.context = context;
        this.utils = new Utils();
        this.services = services;
        this.wifiOnly = wifiOnly;
    }

    /**
     * Register our ActivityLifecycleCallbacks.
     */
    public void start() {
        if(context instanceof Application){
            ((Application) context).registerActivityLifecycleCallbacks(
                    createActivityLifecycleCallback()
            );
        } else {
            utils.logIssue("EventTracker could not start.  Context is not of type Application", null);
        }
    }

    /**
     * Create and return a ActivityLifecycleCallbacks object that tracks all onActivityResumed
     * method calls for all activities.
     */
    Application.ActivityLifecycleCallbacks createActivityLifecycleCallback() {
        return new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                trackActivity(activity);
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        };
    }

    /**
     * Pass activity name to services for Activities that should be tracked.
     */
    void trackActivity(Activity activity) {
        if(shouldTrack(activity)) {
            String activityName = activity.getClass().getSimpleName();
            trackEvent(activity, activityName);
        }
    }

    /**
     * Pass event to log (activity name) to all enabled services.
     * @param context Context
     * @param event Event to track.
     */
    void trackEvent(Context context, String event) {
        for (EventTrackingService service : services) {
            if (service.isEnabled()) {
                service.logEvent(context, event);
            }
        }
    }

    /**
     * Check for classes with @FoamDontTrack annotation.
     * @return true if Activity does not have FoamDontTrack (on class on any of the methods)
     */
    boolean shouldTrack(Activity activity) {
        if(wifiOnly && !utils.isOnWifi(context)){
            return false;
        } else if(activity!=null){
            Class<? extends Activity> clazz = activity.getClass();
            if(clazz.isAnnotationPresent(FoamDontTrack.class)){
                return false;
            } else {
                for (Method method : clazz.getMethods()) {
                    if(method.isAnnotationPresent(FoamDontTrack.class)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
