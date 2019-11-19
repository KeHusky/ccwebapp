package com.example.webapp.dao;

import com.example.webapp.entities.Image;
import com.example.webapp.entities.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, String> {
    List<Recipe> findByAuthorId(String authorId);
}
