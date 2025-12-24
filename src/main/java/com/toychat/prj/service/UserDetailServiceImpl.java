package com.toychat.prj.service;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toychat.prj.entity.User;
import com.toychat.prj.entity.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserService userService;
    
    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    	User user = userService.findByUserId(id);
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        return new UserDetailsImpl(user);
    }
}
