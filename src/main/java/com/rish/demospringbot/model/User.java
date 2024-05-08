package com.rish.demospringbot.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity(name = "userDataTable")
@Data
public class User {

    @Id
    private Long chatId;

    private String firstName;

    private String userName;

    private String lastName;

    private Timestamp registeredAt;
}
