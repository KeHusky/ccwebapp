package com.example.webapp.dao;

import com.example.webapp.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, String> {
    List<Image> findByRecipeId(String recipeId);
}
