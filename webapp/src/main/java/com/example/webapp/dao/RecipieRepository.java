package com.example.webapp.dao;

import com.example.webapp.entities.Recipie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipieRepository extends JpaRepository<Recipie, String> {
}
