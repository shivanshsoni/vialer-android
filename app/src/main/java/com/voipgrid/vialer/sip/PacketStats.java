package com.voipgrid.vialer.sip;

import android.support.annotation.Nullable;

import org.pjsip.pjsua2.StreamStat;

public class PacketStats {

    private final long mSent;
    private final long mReceived;
    private final int mStatsCollectedAt;

    public PacketStats(long sent, long received, int statsCollectedAt) {
        mSent = sent;
        mReceived = received;
        mStatsCollectedAt = statsCollectedAt;
    }

    /**
     * Returns the duration of the sip call when these stats were collected.
     *
     * @return
     */
    public int getStatsCollectedAt() {
        return mStatsCollectedAt;
    }

    /**
     * Get the number of packets that have been sent (the user's voice).
     *
     * @return The number of packets as a long
     */
    public long getSent() {
        return mSent;
    }

    /**
     * Get the number of packets that have been received (the third party's voice).
     *
     * @return The number of packets as a long
     */
    public long getReceived() {
        return mReceived;
    }

    /**
     * Check if there is some audio present at all.
     *
     * @return
     */
    public boolean hasAudio() {
        return !isMissingInboundAudio() || !isNotSendingAudio();
    }

    /**
     * Check if any media has been transmitted for this call.
     *
     * @return If any media has been sent or received TRUE, otherwise FALSE.
     */
    public boolean isMissingAllAudio() {
        return isMissingInboundAudio() && isNotSendingAudio();
    }

    /**
     * Determine if at least one side is lacking media, if TRUE is returned this would
     * suggest there is a problem with the call.
     *
     * @return If sent or received have not transmitted any packets.
     */
    public boolean isOneOrMoreSidesLackingAudio() {
        return isMissingInboundAudio() || isNotSendingAudio();
    }

    /**
     * Check if we have not received any audio at all.
     *
     * @return TRUE if the number of received packets is 0
     */
    public boolean isMissingInboundAudio() {
        return getReceived() == 0;
    }

    /**
     * Check if we have not sent any audio at all.
     *
     * @return TRUE if the number of sent packets is 0
     */
    public boolean isNotSendingAudio() {
        return getSent() == 0;
    }

    @Override
    public String toString() {
        return "PacketStats{" +
                "mSent=" + mSent +
                ", mReceived=" + mReceived +
                ", mStatsCollectedAt=" + mStatsCollectedAt +
                '}';
    }

    public static class Builder {

        /**
         * Builds a packet stats object from a sip call.
         *
         * @param sipCall
         * @return The complete packet stats
         */
        public static @Nullable PacketStats fromSipCall(SipCall sipCall) {
            try {
                StreamStat streamStat = sipCall.getStreamStat(sipCall.getId());

                if (streamStat == null) return null;

                return new PacketStats(
                        streamStat.getRtcp().getTxStat().getPkt(),
                        streamStat.getRtcp().getRxStat().getPkt(),
                        sipCall.getCallDuration()
                );
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
