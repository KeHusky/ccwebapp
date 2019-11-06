package com.example.webapp.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
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
    private String md5;
    private long size;
    private String type;
    private String fileName;
}
