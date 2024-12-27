package com.toychat.prj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.toychat.prj.entity.FcmKey;
import com.toychat.prj.entity.FcmPush;
import com.toychat.prj.repository.FcmKeyRepository;
import com.toychat.prj.repository.FcmPushRepository;

@Service
public class FcmService {
	
	@Autowired
	private FcmKeyRepository fcmKeyRepository;

	@Autowired
	private FcmPushRepository fcmPushRepository;
	
    public void sendNotification(String title, String body, String target) {
    	
    	String token = fcmKeyRepository.findById(target).get().getFcmKey();
    	
    	System.out.println("sent token : " + token);
    	// db 저장
    	FcmPush push = FcmPush.builder()
    				.target(target)
    				.cont(body)
    				.title(title)
    				.chkYn("N")
    				.build();
    	
    	fcmPushRepository.save(push);
    	
    	
    	// send
    	Notification noti = Notification.builder().setTitle(title).setBody(body).build();
        Message message = Message.builder()
            .setToken(token)
            .setNotification(noti)
            .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: " + response);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
    
    

	public void saveFcmKey(FcmKey fcmKey) {
		// merge
		fcmKeyRepository.save(fcmKey);		
	}

}
