package com.goodprod.project.services;


import com.goodprod.project.dtos.UserDto;
import com.goodprod.project.entities.Role;
import com.goodprod.project.entities.User;
import com.goodprod.project.repos.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(UserRepo userRepo, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepo = userRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepo.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException(
                String.format("Пользователь '%s' не найден", login)
        ));
        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                user.getRoles()
        );
    }

    public ResponseEntity<?> createUserService(UserDto userDto){
        if(userRepo.findByLogin(userDto.getLogin()).isEmpty()){
            User user = new User();
            user.setName(userDto.getName());
            user.setSurname(userDto.getSurname());
            user.setLastname(userDto.getLastname());
            user.setGroup_name(userDto.getGroup_name());
            user.setLogin(userDto.getLogin());
            user.setPassword(bCryptPasswordEncoder.encode(userDto.getPassword()));
            if(userDto.getRole().equals("student")){
                user.setRoles(Set.of(Role.ROLE_STUDENT));
            }
            else if(userDto.getRole().equals("teacher")){
                user.setRoles(Set.of(Role.ROLE_TEACHER));
            } else {
                return new ResponseEntity<>("Роль указана не верно", HttpStatus.BAD_REQUEST);
            }
            userRepo.save(user);
            return ResponseEntity.ok("Пользоваетль успешно создан");

        } else {

            return new ResponseEntity<>("Не удалось создать пользователя", HttpStatus.BAD_REQUEST);

        }
    }

    public ResponseEntity<?> createAdmin(){
        User user = new User();
        user.setName("admin");
        user.setLogin("admin");
        user.setPassword(bCryptPasswordEncoder.encode("admin"));
        user.setRoles(Set.of(Role.ROLE_ADMIN));
        userRepo.save(user);
        return ResponseEntity.ok("Админ создан");
    }
}
