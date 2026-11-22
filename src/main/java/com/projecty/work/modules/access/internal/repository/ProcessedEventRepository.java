package com.projecty.work.modules.access.internal.repository;

import java.util.UUID;

import com.projecty.work.modules.access.internal.entity.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

}
