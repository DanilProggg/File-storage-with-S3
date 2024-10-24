package com.goodprod.project.controllers;

import com.goodprod.project.dtos.UserDto;
import com.goodprod.project.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/reg")
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto){
        return userService.createUserService(userDto);
    }

    @GetMapping()
    public ResponseEntity<?> authenticate(Principal principal){
        return new ResponseEntity<>(principal.getName(), HttpStatus.OK);
    }
}
