package com.example.agronode;

/** Run with scripts/test-protocol.sh; no JUnit or Android SDK needed. */
public final class SensorReadingTest {
    private static void invalid(String sample) {
        try { SensorReading.parse(sample); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Accepted malformed packet: " + sample);
    }

    public static void main(String[] args) {
        SensorReading packet = SensorReading.parse("T:25.5,H:68.0,S:43");
        if (packet.temperature != 25.5f || packet.airHumidity != 68f || packet.soilHumidity != 43f) {
            throw new AssertionError("Valid telemetry was parsed incorrectly");
        }
        invalid("T:25.5,H:68");
        invalid("T:25.5,H:68,S:101");
        invalid("T:NaN,H:68,S:43");
        invalid("T:25.5,T:23.0,S:43");
        invalid("T:abc,H:68,S:43");
        invalid("T:25.5,H:68,S:43,FOO:1");
        System.out.println("AgroNode protocol tests passed");
    }
}
