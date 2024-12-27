package com.toychat.prj.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toychat.prj.entity.User;
import com.toychat.prj.entity.UserDetailsImpl;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;
    
    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    	System.out.println("login user");
    	System.out.println(id);
    	User user = userService.findByUserId(id);
        
		System.out.println(user.toString());
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        return new UserDetailsImpl(user);
    }
}
