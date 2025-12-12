package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatsCalculatorTest {

    @Test
    void averageDelayShouldBeCorrect() {
        List<PacketStats> packets = List.of(
                new PacketStats(0, 0L, 0L, 100, 10.0, "t"),
                new PacketStats(1, 0L, 0L, 100, 20.0, "t"),
                new PacketStats(2, 0L, 0L, 100, 30.0, "t")
        );

        double avg = StatsCalculator.averageDelay(packets);
        assertEquals(20.0, avg, 0.0001);
    }

    @Test
    void totalBytesShouldBeCorrect() {
        List<PacketStats> packets = List.of(
                new PacketStats(0, 0L, 0L, 100, 10.0, "t"),
                new PacketStats(1, 0L, 0L, 200, 20.0, "t")
        );

        long total = StatsCalculator.totalBytes(packets);
        assertEquals(300, total);
    }
}
