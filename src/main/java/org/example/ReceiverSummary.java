package org.example;

public class ReceiverSummary {

    private final int expected;
    private final int received;
    private final int lost;
    private final long totalBytes;
    private final double avgDelayMs;

    public ReceiverSummary(int expected, int received, int lost, long totalBytes, double avgDelayMs) {
        this.expected = expected;
        this.received = received;
        this.lost = lost;
        this.totalBytes = totalBytes;
        this.avgDelayMs = avgDelayMs;
    }

    public int getExpected() {
        return expected;
    }

    public int getReceived() {
        return received;
    }

    public int getLost() {
        return lost;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public double getAvgDelayMs() {
        return avgDelayMs;
    }
}
