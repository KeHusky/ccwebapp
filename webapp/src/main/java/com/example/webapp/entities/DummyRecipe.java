package com.example.webapp.entities;

import lombok.Data;

@Data
public class DummyRecipe {
    private String created_ts;
    private String updated_ts;
    private String author_id;
    private String cook_time_in_min;
    private String prep_time_in_min;
    private String total_time_in_min;
    private String title;
    private String cusine;
    private String servings;
    private String[] ingredients;
    private Step[] steps;
    private NutritionInformation nutrition_information;
}
