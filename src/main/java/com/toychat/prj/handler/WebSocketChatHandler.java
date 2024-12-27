package com.toychat.prj.handler;

import java.io.IOException;
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
import com.toychat.prj.repository.ChatRepository;
import com.toychat.prj.service.ChatroomService;

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

	@Resource(name = "Util")
	private Util util;

	private final ObjectMapper mapper = new ObjectMapper();
	private final Set<WebSocketSession> sessions = new HashSet<>();
	private final Map<String, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();
	private final WebSocketSessionManager sessionManager;

	// 소켓 연결 확인
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		System.out.println("===================================================================== afterConnectionEstablished");
		sessions.add(session);
		sessionManager.addSession("chat", session);
	}

	// 소켓 통신 시 메세지의 전송을 다루는 부분
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		System.out.println("===================================================================== handleTextMessage");
		String payload = message.getPayload();

		// 페이로드 -> chatMessageDto로 변환
		Chat chatMessageDto = mapper.readValue(payload, Chat.class);

		// credt put
		String credt = util.getNowDttm();
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

		// sessions 에 넘어온 session 을 담고,
		chatRoomSession.add(session);

		// 입장시
		if (chatMessageDto.getType().equals("ENTER")) {
			// 챗방에 participant 추가
			addParticipant(chatMessageDto);
		} 
		
		// 종료시
		if(chatMessageDto.getType().equals("END")) {
			closeChat(session);
		}

		// 이 부분은 왜 있는거지?
		if (chatRoomSession.size() >= 3) {
			removeClosedSession(chatRoomSession);
		}
		sendMessageToChatRoom(chatMessageDto, chatRoomSession);

		// Redis 저장
		redisTemplate.opsForList().rightPush("chat_" + chatMessageDto.getChatroomId(), chatMessageDto);
	}

	private void addParticipant(Chat chatMessageDto) {
		System.out.println("===================================================================== addParticipant");
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
		System.out.println("===================================================================== afterConnectionClosed");
		System.out.println("종료");
		sessionManager.removeSession("chat", session);
		sessions.remove(session);
	}

	// ====== 채팅 관련 메소드 ======
	private void removeClosedSession(Set<WebSocketSession> chatRoomSession) {
		System.out.println("===================================================================== removeClosedSession");
		chatRoomSession.removeIf(sess -> !sessions.contains(sess));
	}

	private void sendMessageToChatRoom(Chat chatMessageDto, Set<WebSocketSession> chatRoomSession) {
		System.out.println("===================================================================== sendMessageToChatRoom");
		chatRoomSession.parallelStream().forEach(sess -> sendMessage(sess, chatMessageDto));// 2
	}

	public <T> void sendMessage(WebSocketSession session, T message) {
		System.out.println("===================================================================== sendMessage");
		try {
	        if (session != null && session.isOpen()) {  // 세션이 열려 있는지 확인
	            session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
	        } else {
	            System.out.println("WebSocket session is closed. Unable to send message.");
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 실시간 상담 대기 리스트
	private void broadcastActive01Chat() {
		System.out.println("===================================================================== broadcastActive01Chat");
		
		// 관리자 상담 대기리스트 동기화
		Set<WebSocketSession> adminSessions = sessionManager.getSessions("admin");

		List<ChatroomInfo> list = chatroomService.getLiveChatWaitingList(new HashMap<>());
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