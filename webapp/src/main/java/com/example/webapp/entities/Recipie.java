package com.example.webapp.entities;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Recipie {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String ID;
    private String created_ts;
    private String updated_ts;
    private String author_id;
    private int cook_time_in_min;
    private int prep_time_in_min;
    private int total_time_in_min;
    private String title;
    private String cusine;
    private int servings;
    private String ingredients;
    private String steps;
    private String nutrition_information;
}
