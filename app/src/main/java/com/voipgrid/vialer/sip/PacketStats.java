package com.voipgrid.vialer.sip;

public class PacketStats {

    private long sent, received;

    public PacketStats(long sent, long received) {
        this.sent = sent;
        this.received = received;
    }

    /**
     * Get the number of packets that have been sent (the user's voice).
     *
     * @return The number of packets as a long
     */
    public long getSent() {
        return sent;
    }

    /**
     * Get the number of packets that have been received (the third party's voice).
     *
     * @return The number of packets as a long
     */
    public long getReceived() {
        return received;
    }

    /**
     * Check if any media has been transmitted for this call.
     *
     * @return If any media has been sent or received TRUE, otherwise FALSE.
     */
    public boolean hasAnyMediaBeenTransmittedForCall() {
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
                "sent=" + sent +
                ", received=" + received +
                '}';
    }
}
