package com.toychat.prj.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.entity.FcmKey;
import com.toychat.prj.service.FcmService;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {

	@Autowired
	private FcmService fcmService;
	
    @PostMapping("/createKey")
    public void createKey(@RequestBody FcmKey fcmKey) {
        fcmService.saveFcmKey(fcmKey);
    }
    @GetMapping("/sendTest")
    public void sendTest() {
    	
    	String userId = "admin8";
    	String title = "테스트 전송";
    	String body = "테스트 전송 바디";
    	
    	fcmService.sendNotification(title, body, userId);
    }
    
}
