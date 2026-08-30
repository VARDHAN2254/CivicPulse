package com.civicpulse.modules.observability.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Getter
public class CivicPulseMetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter registrationCreatedCounter;
    private final Counter registrationCancelledCounter;
    private final Counter attendanceScannedCounter;
    private final Counter eventCreatedCounter;
    private final Timer qrVerificationTimer;

    public CivicPulseMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.registrationCreatedCounter = Counter.builder("civicpulse.registrations.created")
                .description("Total number of successful event registrations")
                .register(meterRegistry);

        this.registrationCancelledCounter = Counter.builder("civicpulse.registrations.cancelled")
                .description("Total number of cancelled registrations")
                .register(meterRegistry);

        this.attendanceScannedCounter = Counter.builder("civicpulse.attendance.scanned")
                .description("Total number of verified check-ins")
                .register(meterRegistry);

        this.eventCreatedCounter = Counter.builder("civicpulse.events.created")
                .description("Total number of events created across all organizations")
                .register(meterRegistry);

        this.qrVerificationTimer = Timer.builder("civicpulse.qr.verification.time")
                .description("Latency distribution for cryptographic QR check-in verification")
                .register(meterRegistry);
    }

    public void incrementRegistrationCreated() {
        registrationCreatedCounter.increment();
    }

    public void incrementRegistrationCancelled() {
        registrationCancelledCounter.increment();
    }

    public void incrementAttendanceScanned() {
        attendanceScannedCounter.increment();
    }

    public void incrementEventCreated() {
        eventCreatedCounter.increment();
    }

    public void recordQrVerificationTime(long durationMillis) {
        qrVerificationTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}
