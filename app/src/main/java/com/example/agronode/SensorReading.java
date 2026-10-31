package com.example.agronode;

/** One complete ESP32 telemetry frame in the T:<celsius>,H:<percent>,S:<percent> protocol. */
final class SensorReading {
    final float temperature;
    final float airHumidity;
    final float soilHumidity;

    private SensorReading(float temperature, float airHumidity, float soilHumidity) {
        this.temperature = temperature;
        this.airHumidity = airHumidity;
        this.soilHumidity = soilHumidity;
    }

    static SensorReading parse(String line) {
        if (line == null) throw new IllegalArgumentException("Null frame");
        Float temperature = null;
        Float air = null;
        Float soil = null;
        String[] fields = line.trim().split(",", -1);
        if (fields.length != 3) throw new IllegalArgumentException("Expected three fields");

        for (String field : fields) {
            String[] parts = field.trim().split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Malformed field");
            final float value;
            try { value = Float.parseFloat(parts[1].trim()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Non-numeric field", ex); }
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new IllegalArgumentException("Non-finite sensor reading");
            }
            switch (parts[0].trim()) {
                case "T":
                    if (temperature != null || value < -40 || value > 80) throw new IllegalArgumentException("Temperature out of range or repeated");
                    temperature = value;
                    break;
                case "H":
                    if (air != null || value < 0 || value > 100) throw new IllegalArgumentException("Humidity out of range or repeated");
                    air = value;
                    break;
                case "S":
                    if (soil != null || value < 0 || value > 100) throw new IllegalArgumentException("Soil moisture out of range or repeated");
                    soil = value;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown sensor field");
            }
        }
        if (temperature == null || air == null || soil == null) {
            throw new IllegalArgumentException("Missing sensor value");
        }
        return new SensorReading(temperature, air, soil);
    }
}
