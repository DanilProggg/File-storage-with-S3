package com.goodprod.project.entities;



import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ROLE_ADMIN,
    ROLE_TEACHER,
    ROLE_STUDENT;

    @Override
    public String getAuthority() {
        return name();
    }
}
