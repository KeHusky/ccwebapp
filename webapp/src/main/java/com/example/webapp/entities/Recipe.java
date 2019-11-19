package com.example.webapp.entities;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Recipe {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 125)
    private String id;
    private String created_ts;
    private String updatedTs;
    private String authorId;
    private int cook_time_in_min;
    private int prep_time_in_min;
    private int total_time_in_min;
    private String title;
    private String cusine;
    private int servings;
    @Column(length = 1000)
    private String ingredients;
    @Column(length = 1000)
    private String steps;
    @Column(length = 1000)
    private String nutrition_information;
}
