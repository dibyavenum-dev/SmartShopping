package com.smartshopping.notificationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProcessedEvent {

    @Id
    private String eventId;

    private String processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}