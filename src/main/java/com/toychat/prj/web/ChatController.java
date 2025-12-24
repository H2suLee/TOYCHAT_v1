package com.toychat.prj.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.User;
import com.toychat.prj.service.ChatService;
import com.toychat.prj.service.ChatroomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatroomService chatroomService;
    
    @PostMapping("/create")
    public Chatroom createRoom(@RequestBody User user){
        return chatroomService.createRoom(user);
    }

    // 사용자 채팅 이력
    @PostMapping("/chatroomList")
    public List<ChatroomInfo> getChatRoomsByUserId(@RequestBody User user) {
   	 	HashMap<String,Object> searchMap = new HashMap<String, Object>();
   	 	searchMap.put("searchUser", user);
        return chatroomService.getChatRooms(searchMap);
    }    

    // 채팅 상세
    @PostMapping("/chatList")
    public List<Chat> getChatsByChatroomId(@RequestBody Chatroom chatroom) {
    	return chatService.getChatsByChatroomId(chatroom);
    }    

    // 채팅 상세 by redis
    @PostMapping("/liveChatList")
    public List<Object> liveChatList(@RequestBody Chatroom chatroom) {
    	List<Object> returnList = new ArrayList<>();
    	returnList = chatService.getLiveChatsByChatroomId(chatroom);
    	return returnList; 
    }    
    
    @PostMapping("/isNew")
    public boolean isNew(@RequestBody Chat chat){
    	boolean isNew = chatroomService.isNewParticipant(chat);
    	return isNew;
    }

}
