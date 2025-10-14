package com.toychat.prj.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toychat.prj.common.sequence.SequenceService;
import com.toychat.prj.common.util.Util;
import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.FcmPush;
import com.toychat.prj.repository.ChatRepository;
import com.toychat.prj.service.ChatroomService;
import com.toychat.prj.service.FcmService;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {

	@Autowired
	private SequenceService sequenceService;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private ChatroomService chatroomService;
	
	@Autowired
	private FcmService fcmService;
	
	private final ObjectMapper mapper = new ObjectMapper();
	private final Set<WebSocketSession> sessions = new HashSet<>();
	private final Map<String, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();
	private final WebSocketSessionManager sessionManager;

	// 소켓 연결 확인
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.debug("===================================================================== afterConnectionEstablished : " + session);
		sessions.add(session);
		sessionManager.addSession("chat", session);
	}

	// 소켓 통신 시 메세지의 전송을 다루는 부분
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String payload = message.getPayload();
		log.debug("===================================================================== handleTextMessage" + payload);

		// 페이로드 -> chatMessageDto로 변환
		Chat chatMessageDto = mapper.readValue(payload, Chat.class);

		// credt put
		String credt = Util.getNowDttm();
		chatMessageDto.setCredt(credt);

		// chatId put
		String chatId = String.valueOf(sequenceService.generateSequence(Chat.SEQUENCE_NAME));
		chatMessageDto.setChatId(chatId);

		String chatRoomId = chatMessageDto.getChatroomId();

		// 세션에 chatRoomId 저장
		session.getAttributes().put("chatRoomId", chatRoomId);

		// 메모리 상에 채팅방에 대한 세션 없으면 만들어줌
		if (!chatRoomSessionMap.containsKey(chatRoomId)) {
			chatRoomSessionMap.put(chatRoomId, new HashSet<>());
		}
		Set<WebSocketSession> chatRoomSession = chatRoomSessionMap.get(chatRoomId);

		// sessions 에 넘어온 session 을 담고, closed session 정리
		chatRoomSession.add(session);
		removeClosedSession(chatRoomSession);

		log.debug("size: " + chatRoomSession.size());
		// 입장시
		if (chatMessageDto.getType().equals("ENTER")) {
			// 챗방에 participant 추가
			addParticipant(chatMessageDto);
		} 
		
		// 종료시
		if(chatMessageDto.getType().equals("END")) {
			closeChat(session);
		}
		
		// 복구시 밑에 로직을 안탐, 소켓 연결만이 목적..
		if(chatMessageDto.getType().equals("REJOIN")) {
			return;
		}
		
		sendMessageToChatRoom(chatMessageDto, chatRoomSession);

		// Redis 저장
		if(!chatMessageDto.getType().equals("TYPING") && !chatMessageDto.getType().equals("STOP") ) {
			redisTemplate.opsForList().rightPush("chat_" + chatMessageDto.getChatroomId(), chatMessageDto);
		}
	}

	private void addParticipant(Chat chatMessageDto) {
		log.debug("===================================================================== addParticipant");
		String status = chatroomService.addParticipant(chatMessageDto);

		// 실시간 상담리스트 동기화
		broadcastActive01Chat();
	}
	
	private void closeChat(WebSocketSession session) {
		// 해당 session의 chatroomId 를 들고와야함..
		String chatRoomId = (String) session.getAttributes().get("chatRoomId");
		
		try {
			if (chatRoomId != null) {

				// 채팅방 상태 업데이트
				chatroomService.closeChatroom(chatRoomId);

				// 실시간 상담리스트 동기화
				broadcastActive01Chat();

				// Redis에서 메시지를 가져와 MongoDB에 저장
				List<Object> messages = redisTemplate.opsForList().range("chat_" + chatRoomId, 0, -1);
				if (messages != null && !messages.isEmpty()) {
					for (Object obj : messages) {
						Chat message = (Chat) obj;
						chatRepository.save(message);
					}
					redisTemplate.delete("chat_" + chatRoomId);
				}

			}

		} catch (Exception e) {

		}
	}

	// 소켓 종료 확인
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		log.debug("===================================================================== afterConnectionClosed");
		log.debug("종료");
		sessionManager.removeSession("chat", session);
		sessions.remove(session);
	}

	// ====== 채팅 관련 메소드 ======
	private void removeClosedSession(Set<WebSocketSession> chatRoomSession) {
		log.debug("===================================================================== removeClosedSession");
		chatRoomSession.removeIf(sess -> !sessions.contains(sess));
	}

	private void sendMessageToChatRoom(Chat chatMessageDto, Set<WebSocketSession> chatRoomSession) {
		log.debug("===================================================================== sendMessageToChatRoom : ");
		chatRoomSession.parallelStream().forEach(sess -> {
			
			String sessionRoomId = (String) sess.getAttributes().get("chatRoomId");

			if(sessionRoomId.equals(chatMessageDto.getChatroomId())) {
				if("TALK".equals(chatMessageDto.getType())) {
				}
			}else {
				if("TALK".equals(chatMessageDto.getType())) {
					chatMessageDto.setType("LIST");
				}else if("TYPING".equals(chatMessageDto.getType())|| "STOP".equals(chatMessageDto.getType())) {
					return;
				}
			}
			sendMessage(sess, chatMessageDto);
			
		});// 2
		
		// fcm push
		boolean result = chatRoomSession.parallelStream().anyMatch(sess -> {
			String sessionRoomId = (String) sess.getAttributes().get("chatRoomId");
			if(sessionRoomId.equals(chatMessageDto.getChatroomId())) {
				if("TALK".equals(chatMessageDto.getType())) {
					// chat type talk 이면
					log.debug(sessionRoomId + " fireBase send 필요");
					// firebase type01 알람
					String title = "[채팅 알람]";
					String cont = chatMessageDto.getContent();
					String chatroomId = chatMessageDto.getChatroomId();
					String sender = chatMessageDto.getId();
					
					FcmPush msg = FcmPush.builder()
							.type("type00")
							.cont(chatMessageDto.getNick() + ": " + cont)
							.title(title)
							.sender(sender)
							.chatroomId(chatroomId)
							.build();
			    	
			    	fcmService.sendNotification(msg);
			    	return true;
				}
			}
			return false;
		});
	}

	public <T> void sendMessage(WebSocketSession session, T message) {
		log.debug("===================================================================== sendMessage : " + session );
		try {
	        if (session != null && session.isOpen()) {  // 세션이 열려 있는지 확인
	            session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
	        } else {
	            log.debug("WebSocket session is closed. Unable to send message.");
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 실시간 상담 대기 리스트
	private void broadcastActive01Chat() {
		log.debug("===================================================================== broadcastActive01Chat");
		// 관리자 상담 대기리스트 동기화
		Set<WebSocketSession> adminSessions = sessionManager.getSessions("admin");
		HashMap<String,Object> searchMap = new HashMap<String, Object>();
		List<String> searchStatus =  Arrays.asList("01", "02");
    	searchMap.put("searchStatus", searchStatus);
		List<ChatroomInfo> list = chatroomService.getChatRooms(searchMap);
		String jsonList = null;
		try {
			jsonList = mapper.writeValueAsString(list);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		for (WebSocketSession adminSession : adminSessions) {
			String nick = (String) adminSession.getAttributes().get("nick");
			// 관리자에게만 보냄
			if (adminSession.isOpen() && nick != null) {
				try {
					adminSession.sendMessage(new TextMessage(jsonList));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}