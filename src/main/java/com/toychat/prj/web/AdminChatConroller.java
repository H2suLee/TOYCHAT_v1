package com.toychat.prj.web;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.User;
import com.toychat.prj.entity.UserDetailsImpl;
import com.toychat.prj.handler.WebSocketChatHandler;
import com.toychat.prj.service.ChatService;
import com.toychat.prj.service.ChatroomService;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatConroller {
	
	@Autowired
	private WebSocketChatHandler webSocketChatHandler;
	
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ChatroomService chatroomService;
    
    // 관리자 채팅 이력 : 03 인 [카테고리, 상태(진행중/완료), 채팅방 생성일, 채팅방 수정일, 문의자, 관리(메모)]
    @PostMapping("/mnglist")
    public List<ChatroomInfo> getChatRoomsMngList(@RequestBody HashMap<String,Object> searchMap) {
        // id
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String id = userDetails.getUsername();
        
        return chatroomService.getChatRoomsMngList(searchMap);
    }    
    
    // 채팅 대기 리스트 : 상태가 01, 02인 [생성일, 사용자 이름, 상태]
    @PostMapping("/liveChatWaitingList")
    public List<ChatroomInfo> getLiveChatWaitingList(@RequestBody HashMap<String,Object> searchMap) {
    	return chatroomService.getLiveChatWaitingList(searchMap);
    }     

    // 채팅 관리 정보 : category, memo
    @PostMapping("/chatManageInfo")
    public Chatroom getChatManageInfo(@RequestBody Chatroom chatroom) {
    	return chatroomService.getChatManageInfo(chatroom);
    }     

    // 채팅 관리 저장 : category, memo
    @PostMapping("/saveChatManageInfo")
    public void saveChatManageInfo(@RequestBody Chatroom chatroom) {
    	chatroomService.saveChatManageInfo(chatroom);
    }     

    // 내 이력 : 상태가 02, 03인 [생성일, 사용자 이름, 상태]
    @PostMapping("/mylist")
    public List<ChatroomInfo> getChatRoomsByUserId(@RequestBody User user) {
        return chatroomService.getChatRoomsByUserId(user);
    }       
    

}
