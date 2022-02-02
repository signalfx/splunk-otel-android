package com.splunk.rum;

class SessionIdInactivityTimeoutListener implements AppStateListener {

    private final SessionId sessionId;

    SessionIdInactivityTimeoutListener(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void appForegrounded() {
        // bringing the app back should trigger span creation; which in turn should reset the timeout
    }

    @Override
    public void appBackgrounded() {
        sessionId.startInactivityTimeout();
    }
}
