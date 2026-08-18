package com.smartshopping.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshopping.notificationservice.entity.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, String> {
}