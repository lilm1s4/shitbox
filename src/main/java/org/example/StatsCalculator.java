package org.example;

import java.util.List;

public class StatsCalculator {

    public static double averageDelay(List<PacketStats> packets) {
        return packets.stream()
                .mapToDouble(PacketStats::getDelayMs)
                .average()
                .orElse(0.0);
    }

    public static long totalBytes(List<PacketStats> packets) {
        return packets.stream()
                .mapToLong(PacketStats::getSizeBytes)
                .sum();
    }
}
