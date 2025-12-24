package com.toychat.prj.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.User;
import com.toychat.prj.service.ChatroomService;

import lombok.RequiredArgsConstructor;
 
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
public class AdminChatConroller {
	
    private final ChatroomService chatroomService;
    
    @PostMapping("/mnglist")
    public List<ChatroomInfo> getChatRoomsMngList(@RequestBody HashMap<String,Object> searchMap) {
    	List<String> searchStatus =  Arrays.asList("03");
    	searchMap.put("searchStatus", searchStatus);
    	searchMap.put("searchMinimumParticipantsSize", "2");
    	List<ChatroomInfo> resultList = chatroomService.getChatRooms(searchMap);
        return resultList;
    }    
    
    @PostMapping("/liveChatWaitingList")
    public List<ChatroomInfo> getLiveChatWaitingList(@RequestBody HashMap<String,Object> searchMap) {
    	List<String> searchStatus =  Arrays.asList("01", "02");
    	searchMap.put("searchStatus", searchStatus);
    	return chatroomService.getChatRooms(searchMap);
    }     

    @PostMapping("/chatManageInfo")
    public Chatroom getChatManageInfo(@RequestBody Chatroom chatroom) {
    	return chatroomService.getChatManageInfo(chatroom);
    }     

    @PostMapping("/saveChatManageInfo")
    public void saveChatManageInfo(@RequestBody Chatroom chatroom) {
    	chatroomService.saveChatManageInfo(chatroom);
    }     

    @PostMapping("/mylist")
    public List<ChatroomInfo> getChatRoomsByUserId(@RequestBody User user) {
    	 HashMap<String,Object> searchMap = new HashMap<String, Object>();
    	 searchMap.put("searchUser", user);
        return chatroomService.getChatRooms(searchMap);
    }       
    

}
