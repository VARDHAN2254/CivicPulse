package com.civicpulse.modules.observability.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CivicPulseMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private CivicPulseMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new CivicPulseMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should increment custom business counters")
    void testIncrementCounters() {
        metricsService.incrementRegistrationCreated();
        metricsService.incrementRegistrationCreated();
        metricsService.incrementRegistrationCancelled();
        metricsService.incrementAttendanceScanned();
        metricsService.incrementEventCreated();

        assertThat(metricsService.getRegistrationCreatedCounter().count()).isEqualTo(2.0);
        assertThat(metricsService.getRegistrationCancelledCounter().count()).isEqualTo(1.0);
        assertThat(metricsService.getAttendanceScannedCounter().count()).isEqualTo(1.0);
        assertThat(metricsService.getEventCreatedCounter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record timer latency distribution")
    void testRecordTimer() {
        metricsService.recordQrVerificationTime(45);
        metricsService.recordQrVerificationTime(65);

        assertThat(metricsService.getQrVerificationTimer().count()).isEqualTo(2);
        assertThat(metricsService.getQrVerificationTimer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(110.0);
    }
}
