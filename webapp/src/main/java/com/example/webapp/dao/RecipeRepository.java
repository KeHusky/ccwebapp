package com.example.webapp.dao;

import com.example.webapp.entities.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, String> {
}
