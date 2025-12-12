package org.example;

public class PacketStats {

    private final int sequence;
    private final long sendTimeMs;
    private final long recvTimeMs;
    private final int sizeBytes;
    private final double delayMs;
    private final String note;

    public PacketStats(
            int sequence,
            long sendTimeMs,
            long recvTimeMs,
            int sizeBytes,
            double delayMs,
            String note
    ) {
        this.sequence = sequence;
        this.sendTimeMs = sendTimeMs;
        this.recvTimeMs = recvTimeMs;
        this.sizeBytes = sizeBytes;
        this.delayMs = delayMs;
        this.note = note;
    }

    public int getSequence() {
        return sequence;
    }

    public long getSendTimeMs() {
        return sendTimeMs;
    }

    public long getRecvTimeMs() {
        return recvTimeMs;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public double getDelayMs() {
        return delayMs;
    }

    public String getNote() {
        return note;
    }
}
