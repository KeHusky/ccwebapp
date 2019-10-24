package com.example.webapp.entities;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Image {
    @Id
    @Column(length = 125)
    private String id;
    @Column(length = 1000)
    private String url;
    private String recipeId;
}
