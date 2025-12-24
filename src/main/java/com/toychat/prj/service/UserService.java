package com.toychat.prj.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.toychat.prj.entity.User;
import com.toychat.prj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void delete(String id) {
        userRepository.deleteById(id);
    }
    
    public User findChatroomById(String id){
    	return userRepository.findChatroomById(id);
    }

	public User findByUserId(String id) {
	    Optional<User> userOptional = userRepository.findById(id);
	    return userOptional.orElse(null);
	}
}