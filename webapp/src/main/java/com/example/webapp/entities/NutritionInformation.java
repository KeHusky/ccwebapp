package com.example.webapp.entities;

import lombok.Data;

@Data
public class NutritionInformation {
    private int calories;
    private Number cholesterol_in_mg;
    private int sodium_in_mg;
    private Number carbohydrates_in_grams;
    private Number protein_in_grams;
}
