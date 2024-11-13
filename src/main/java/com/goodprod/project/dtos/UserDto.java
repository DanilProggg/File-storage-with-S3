package com.goodprod.project.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String name;
    private String surname;
    private String lastname;
    private String group_name;
    private String role;

    private String login;
    private String password;
}