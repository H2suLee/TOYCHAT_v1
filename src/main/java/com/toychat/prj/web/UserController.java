package com.toychat.prj.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.entity.User;
import com.toychat.prj.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
	
    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @PostMapping("/save")
    public User createUser(@RequestBody User user) {
//    	user.setId("test01");
//    	user.setNick("테스트유저1");
//    	user.setPw("test1234");
//    	user.setRole("USR");
        return userService.save(user);
    }
    
    @PostMapping("/login")
    public User login(@RequestBody User user) {
//    	user.setId("test01");
//    	user.setNick("테스트유저1");
//    	user.setPw("test1234");
//    	user.setRole("USR");
        return userService.save(user);
    }    

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.delete(id);
    }
}
