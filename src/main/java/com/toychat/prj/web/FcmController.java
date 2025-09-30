package com.toychat.prj.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.entity.FcmKey;
import com.toychat.prj.entity.FcmPush;
import com.toychat.prj.service.FcmService;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {

	@Autowired
	private FcmService fcmService;
	
    @PostMapping("/createKey")
    public String createKey(@RequestBody FcmKey fcmKey) {
    	try {
    		fcmService.saveFcmKey(fcmKey);
		} catch (DuplicateKeyException e) {
			System.out.println("duplicate key");
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return "User registered successfully";
    }

    @PostMapping("/deleteKey")
    public String deleteKey(@RequestBody FcmKey fcmKey) {
    	System.out.println("delete Key");
    	try {
    		fcmService.deleteFcmKey(fcmKey);
    	} catch (DuplicateKeyException e) {
    		System.out.println("duplicate key");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return "User registered successfully";
    }

    @PostMapping("/listFcmPush")
    public List<FcmPush> listFcmPush(@RequestBody FcmPush vo) {
    	List<FcmPush> resultList = new ArrayList<FcmPush>();
    	System.out.println("listFcm");
    	try {
    		resultList = fcmService.selectFcmPushList(vo);
    		System.out.println("vo : " + vo.toString() + "listFcm size : " + resultList.size());
    	}catch (Exception e) {
    		e.printStackTrace();
    	}
    	return resultList;
    }
    
    @GetMapping("/sendTest")
    public void sendTest() {
    	
    	String userId = "admin6";
    	String title = "테스트 전송";
    	String body = "테스트 전송 바디";

    	FcmPush msg = FcmPush.builder()
				.target(userId)
				.cont(body)
				.title(title)
				.build();
    	
    	fcmService.sendNotification(msg);
    }
    
}
