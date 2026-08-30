package com.civicpulse.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_EVENTS_LIFECYCLE = "civicpulse.events.lifecycle";
    public static final String TOPIC_REGISTRATIONS = "civicpulse.registrations";
    public static final String TOPIC_ATTENDANCE = "civicpulse.attendance";
    public static final String TOPIC_ANNOUNCEMENTS = "civicpulse.announcements";
    public static final String TOPIC_NOTIFICATIONS = "civicpulse.notifications";
    public static final String TOPIC_MODERATION = "civicpulse.moderation";
    public static final String TOPIC_AUDIT = "civicpulse.audit";

    @Bean
    public NewTopic eventsLifecycleTopic() {
        return TopicBuilder.name(TOPIC_EVENTS_LIFECYCLE)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic registrationsTopic() {
        return TopicBuilder.name(TOPIC_REGISTRATIONS)
                .partitions(12)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic attendanceTopic() {
        return TopicBuilder.name(TOPIC_ATTENDANCE)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic announcementsTopic() {
        return TopicBuilder.name(TOPIC_ANNOUNCEMENTS)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic moderationTopic() {
        return TopicBuilder.name(TOPIC_MODERATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(TOPIC_AUDIT)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
