package com.example.webapp.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class Metadata {
    @Id
    @Column(length = 125)
    private String image_id;
    private String md5;
    private long size;
    private String type;
}
