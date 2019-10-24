package com.example.webapp.dao;

import com.example.webapp.entities.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataRepository extends JpaRepository<Metadata, String> {
}
