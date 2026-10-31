#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
OUTPUT="$(mktemp -d)"
trap 'rm -rf "$OUTPUT"' EXIT HUP INT TERM
javac -encoding UTF-8 -d "$OUTPUT" app/src/main/java/com/example/agronode/SensorReading.java tests/com/example/agronode/SensorReadingTest.java
java -cp "$OUTPUT" com.example.agronode.SensorReadingTest
