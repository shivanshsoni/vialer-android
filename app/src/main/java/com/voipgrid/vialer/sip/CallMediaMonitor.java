package com.voipgrid.vialer.sip;

import android.util.Log;

import com.voipgrid.vialer.logging.RemoteLogger;

import org.pjsip.pjsua2.CallOpParam;

public class CallMediaMonitor implements Runnable {

    private final SipCall mSipCall;
    private final RemoteLogger mRemoteLogger;

    /**
     * Track the packet stats at select intervals so we can send
     * reinvites if the audio appears to have stopped during a
     * call.
     */
    private PacketStats mTrackedIntervalPacketStats;

    /**
     * The delay between checking the packet stats in milliseconds.
     */
    private static final int CHECK_EVERY_MS = 1000;

    /**
     * The frequency at which the current packet stats for a call should
     * be reported to the logger, this occurs when there is audio, in seconds. This
     * will not cause stats to be reported any quicker than {@value CHECK_EVERY_MS}.
     */
    private static final int REPORT_INTERVAL_IN_SECONDS = 10;

    /**
     * A reinvite will be sent if there is no audio detected in this
     * interval.
     */
    private static final int REINVITE_IF_NO_AUDIO_IN_SECONDS = 5;

    CallMediaMonitor(SipCall sipCall) {
        mSipCall = sipCall;
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
    }

    @Override
    public void run() {
        while (shouldBeMonitoringMedia()) {
            PacketStats packetStats = mSipCall.getMediaPacketStats();

            if (packetStats == null) break;

            handleMediaPacketStats(packetStats);

            sleep(CHECK_EVERY_MS);
        }
    }

    /**
     * Analyse the packet stats and either log information about them or
     * send a re-invite if there is no audio at all.
     */
    private void handleMediaPacketStats(PacketStats packetStats) {
        // If all audio is missing from a call then send a reinvite
        if (packetStats.isMissingAllAudio()) {
            mRemoteLogger.w("There is NO audio " + mSipCall.getCallDuration()
                    + " sec into the call. Trying a reinvite");
            attemptCallReinvite();
            return;
        }

        if (shouldTrackThesePacketStats()) {
            // If there has been no audio since our last interval this means that audio may have
            // dropped and we should send a reinvite.
            if (mTrackedIntervalPacketStats != null && hasAudioSinceLastTrackedStats(packetStats)) {
                mRemoteLogger.w("There has been NO audio between "
                        + mTrackedIntervalPacketStats.getStatsCollectedAt() + "s and "
                        + packetStats.getStatsCollectedAt() + "s. Trying a reinvite");
                attemptCallReinvite();
            }
            mTrackedIntervalPacketStats = packetStats;
        }

        if (packetStats.hasAudio() && isTimeToReportAudioStats()) {
            mRemoteLogger.i(
                    "There is audio in the last " + REPORT_INTERVAL_IN_SECONDS + " seconds rxPkt: "
                            + packetStats.getReceived() + " and txPkt: " + packetStats.getSent());
        }
    }

    /**
     * Compare current packet stats with the last tracked stats to see if there has been any
     * audio in this interval.
     *
     * @return TRUE if there has been some audio (in either direction) since the last tracked stats.
     */
    private boolean hasAudioSinceLastTrackedStats(PacketStats currentPacketStats) {
        return (currentPacketStats.getReceived() - mTrackedIntervalPacketStats.getReceived() <= 0)
                ||
                (currentPacketStats.getSent() - mTrackedIntervalPacketStats.getSent()) <= 0;
    }

    /**
     * Attempts to send a reinvite for the call, if this fails then a message
     * is logged.
     */
    private void attemptCallReinvite() {
        try {
            mSipCall.reinvite(new CallOpParam(true));
        } catch (Exception e) {
            mRemoteLogger.e("Unable to reinvite call: " + e.getMessage());
        }
    }

    /**
     * Check if we should track the current packet stats, this means updating
     * the tracked stats
     *
     * @return TRUE if stats should be reported, otherwise false.
     */
    private boolean shouldTrackThesePacketStats() {
        if (mSipCall.getCallDuration() == 0) {
            return false;
        }

        return (mSipCall.getCallDuration() % REINVITE_IF_NO_AUDIO_IN_SECONDS) == 0;
    }

    /**
     * Check the current call duration to determine if we should be reporting stats or not,
     * it should be checked ever {@value REPORT_INTERVAL_IN_SECONDS} seconds.
     *
     * @return TRUE if stats should be reported, otherwise false.
     */
    private boolean isTimeToReportAudioStats() {
        if (mSipCall.getCallDuration() == 0) {
            return false;
        }

        return (mSipCall.getCallDuration() % REPORT_INTERVAL_IN_SECONDS) == 0;
    }

    /**
     * Check that the call is still alive, if it is we should be monitoring it.
     *
     * @return TRUE if we should be monitoring the call media, otherwise false.
     */
    private boolean shouldBeMonitoringMedia() {
        return mSipCall != null && mSipCall.getIsCallConnected();
    }

    /**
     * Sleep the current thread for the given number of milliseconds.
     *
     * @param milliseconds The number of milliseconds to sleep the current thread for.
     */
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
