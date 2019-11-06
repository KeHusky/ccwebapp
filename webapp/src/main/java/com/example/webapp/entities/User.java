package com.example.webapp.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class User {
    private String id;
    @Id
    @Column(length = 125)
    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String account_created;
    private String account_updated;
}
