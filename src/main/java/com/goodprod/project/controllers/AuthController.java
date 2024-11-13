package com.goodprod.project.controllers;

import com.goodprod.project.dtos.AuthResponse;
import com.goodprod.project.dtos.UserDto;
import com.goodprod.project.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;


    public AuthController(UserService userService) {
        this.userService = userService;
    }


    //ADMIN ONLY
    @PostMapping("/reg")
    public ResponseEntity<?> createUser(Principal principal, @RequestBody UserDto userDto){
        return userService.createUserService(userDto);
    }

    @GetMapping()
    public ResponseEntity<?> authenticate(Principal principal){
        return new ResponseEntity<>(new AuthResponse(true), HttpStatus.OK);
    }

    @GetMapping("/create-admin")
    public ResponseEntity<?> createAdmin(){
        return userService.createAdmin();
    }
}
