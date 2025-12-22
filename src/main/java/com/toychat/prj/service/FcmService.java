package com.toychat.prj.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.toychat.prj.common.util.Util;
import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.FcmKey;
import com.toychat.prj.entity.FcmPush;
import com.toychat.prj.entity.Participant;
import com.toychat.prj.entity.User;
import com.toychat.prj.repository.FcmKeyRepository;
import com.toychat.prj.repository.FcmPushRepository;
import com.toychat.prj.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {
	
	private final FcmKeyRepository fcmKeyRepository;
	private final FcmPushRepository fcmPushRepository;
	private final UserRepository userRepository;
	private final MongoTemplate mongoTemplate;

	
    public void sendNotification(FcmPush msg) {
    	
    	//String target = msg.getTarget();
    	String title = msg.getTitle();
    	String body = msg.getCont();
    	String chatroomId = msg.getChatroomId();
    	String type = msg.getType();
    	String sender = msg.getSender();
    	
    	List<FcmKey> keyList = getKeyList(msg);
    	List<String> tokenList = keyList.stream().map(FcmKey::getToken).distinct().toList();
    	List<String> targetList = keyList.stream().map(FcmKey::getUserId).distinct().toList();
    	String credt = Util.getNowDttm();
    	
    	
    	Notification noti = Notification.builder().setTitle(title).setBody(body).build();
    	log.debug("수신키 : " + tokenList.toString());
    	try {

    		// send 단건
    		for (String token : tokenList) {
    			
    			Message message = Message.builder()
    					.setToken(token)
    					//.setNotification(noti)
    					.putData("title", title)
    			        .putData("body", body)
    			        .putData("chatroomId", chatroomId)
    			        .putData("credt", credt)
    			        .putData("type", type)
    			        .putData("sender", sender)
    					.build();
    			String response = FirebaseMessaging.getInstance().send(message);
    			log.debug("Successfully sent message: " + response);
    		}
    		
    		// db 저장
    		/*
    		for(String target : targetList) {
    			FcmPush push = FcmPush.builder()
    					.target(target)
    					.cont(body)
    					.title(title)
    					.chkYn("N")
    					.delYn("N")
    					.credt(credt)
    					.build();
    			
    			fcmPushRepository.save(push);
    		}
    		 * */
    		// send multi.. 버젼이슈로 404 에러가 남
    		/*
    		MulticastMessage multimessage = MulticastMessage.builder()
        		.addAllTokens(tokenList)
        		.setNotification(noti)
                .build();
    		 * */

        	//BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multimessage);
        	//log.debug("성공: " + response.getSuccessCount() + ", 실패: " + response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }


	private List<FcmKey> getKeyList(FcmPush msg) {
		List<FcmKey> resultList = null;
		
		// target을 추출하여 그 target 의 fcmkey list를 추출
		String type = msg.getType();
		
		// type 분기처리
		List<String> targets = null;
		if("type00".equals(type)) {
			// type00 : 채팅방에 소속된 참여자 중 sender 빼고 모두
			String sender = msg.getSender();
			String chatroomId = msg.getChatroomId();
			targets = getPushTargetsByChatroom(sender, chatroomId);
			
		}else if("type01".equals(type)) {
			// type01 : 관리자 모두
			List<User> users = userRepository.findByRole("ROLE_ADM");
			targets = users.stream().map(User::getId).toList();
		}
		
		if (targets != null) {
			resultList = fcmKeyRepository.findByUserIdIn(targets);
		}

		return resultList;
	}


	private List<String> getPushTargetsByChatroom(String sender, String chatroomId) {
		Query query = new Query(Criteria.where("_id").is(chatroomId));
	    Chatroom chatroom = mongoTemplate.findOne(query, Chatroom.class, "chatrooms");
	    
	    if (chatroom == null || chatroom.getParticipants() == null) {
	        return Collections.emptyList();
	    }
	    
	    return chatroom.getParticipants().stream()
	            .filter(p -> !sender.equals(p.getId()))
	            .map(Participant::getId)
	            .collect(Collectors.toList());
	}


	public void saveFcmKey(FcmKey fcmKey) {
		// 다른 계정으로 같은 토큰이 있으면 삭제 delete from fcmKey where token = {token} and userId != {userId};
		removeTokenbyId(fcmKey);
		fcmKeyRepository.save(fcmKey);		
	}


	private void removeTokenbyId(FcmKey fcmKey) {
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(fcmKey.getUserId()).and("token").is(fcmKey.getToken()));
		mongoTemplate.remove(query, FcmKey.class);
	}


	public void deleteFcmKey(FcmKey fcmKey) {
		 Query query = new Query();
		 query.addCriteria(Criteria.where("token").is(fcmKey.getToken()));
		 mongoTemplate.remove(query, FcmKey.class);
	}


	public List<FcmPush> selectFcmPushList(FcmPush vo) {
		return fcmPushRepository.findByTarget(vo.getTarget());
	}

}
