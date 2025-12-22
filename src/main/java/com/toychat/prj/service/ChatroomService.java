package com.toychat.prj.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.lookup;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.toychat.prj.common.sequence.SequenceService;
import com.toychat.prj.common.util.Util;
import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.FcmPush;
import com.toychat.prj.entity.Participant;
import com.toychat.prj.entity.User;
import com.toychat.prj.repository.ChatroomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatroomService {

	private final MongoTemplate mongoTemplate;
	private final SequenceService sequenceService;
	private final ChatroomRepository chatroomRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final FcmService fcmService;

	// 채팅방 생성
	public Chatroom createRoom(User user) {
		 // roomId uuid 생성
		 String roomId = String.valueOf(sequenceService.generateSequence(Chatroom.SEQUENCE_NAME));
		 // credt YYYY-MM-DD HH24:NN:DD 생성
        String credt = Util.getNowDttm();

        // Chatroom build
	     Chatroom room = Chatroom.builder()
	                .chatroomId(roomId)
	                .credt(credt)
	                .status("01")
	                //.participants(participants)
	                .build();
	     
	     
	     // chatroom에 채팅방 등록
	     chatroomRepository.save(room);

         return room;
	}

	// 채팅방 리스트
	public List<ChatroomInfo> getChatRooms(HashMap<String, Object> searchMap) {
		List<ChatroomInfo> list = null;

		// 검색 조건 세팅
		Criteria criteria = Criteria.where("participants").ne(null);

		User searchUser = (User) searchMap.get("searchUser");
		if (searchUser != null) {
			String userId = searchUser.getId();
			criteria.and("participants._id").is(userId);
		}

		List<String> searchStatus = (List<String>) searchMap.get("searchStatus");
		if (searchStatus != null) {
			criteria.and("status").in(searchStatus);
		}

		MatchOperation matchOperation = match(criteria);

		// projection
		ProjectionOperation addParticipantsSizeProjection = project().and("_id").as("_id")
				.and(ArrayOperators.ArrayElemAt.arrayOf("participants").elementAt(0)).as("usr")
				.and((ConditionalOperators.when(
				        ComparisonOperators.Gte.valueOf(
				                ArrayOperators.Size.lengthOfArray("participants")
				            ).greaterThanEqualToValue(2)
				        )
						.thenValueOf("participants")
				        .otherwise(new ArrayList<>()))).as("adm")
				.and("status").as("status")
				.and("credt").as("credt")
				.and(ArrayOperators.Size
						.lengthOfArray(ConditionalOperators.ifNull("participants").then(new ArrayList<>())))
				.as("participantsSize");

		MatchOperation filterByParticipantsSize = match(Criteria.where("participantsSize").gte(1));
		if(searchMap.get("searchMinimumParticipantsSize") != null) {
			filterByParticipantsSize = match(Criteria.where("participantsSize").gte(2));
		}

		// Lookup을 사용하여 "chats" 컬렉션에서 데이터 조회
		LookupOperation lookupOperation = lookup("chats", "_id", "chatroomId", "lastMessages");

		// lastMessages 배열을 unwind 하여 각 채팅방의 마지막 메시지를 가져옵니다.
		UnwindOperation unwindOperation = unwind("lastMessages", true);

		// credt 내림차순
		SortOperation sortOperation = sort(Sort.by(Sort.Direction.DESC, "lastMessages.credt"));

		// 필요 필드 그룹화
		GroupOperation groupOperation = group("_id")
				.first("_id").as("chatroomId")
				.first("usr").as("usr")
				.first("adm").as("adm")
				.first("credt").as("credt")
				.first("status").as("status")
				.first("lastMessages.content").as("lastContent")
				.first("lastMessages.type").as("lastChatType")
				.first("lastMessages.credt").as("lastCredt");

		// 상태, 마지막 채팅일시 내림차순
		SortOperation finalSortOperation = sort(Sort.by(Sort.Order.asc("status"), Sort.Order.desc("lastCredt")));

		// Aggregation 파이프라인을 설정
		Aggregation aggregation = newAggregation(matchOperation, addParticipantsSizeProjection,
				filterByParticipantsSize, lookupOperation, unwindOperation, sortOperation, groupOperation, finalSortOperation);

		// Aggregation 실행
		AggregationResults<ChatroomInfo> results = mongoTemplate.aggregate(aggregation, "chatrooms",ChatroomInfo.class);
		list = results.getMappedResults();

		// mongo aggregation으로 힘든거 java로 재가공 : redis 마지막 메시지 매핑하기, adm 가공
		list = getRedisLastMessageMapped(list);
		return list;
	}
	
	private List<ChatroomInfo> getRedisLastMessageMapped(List<ChatroomInfo> list) {
		Chat chatMessageDto = null;
        for (ChatroomInfo room : list) {
		    String rid = room.getChatroomId();
		    
		    // redis 마지막 메시지 매핑하기
		    if (!"03".equals(room.getStatus())) {
		    	chatMessageDto = (Chat) redisTemplate.opsForList().index("chat_" + rid, -1);
		    	if(chatMessageDto != null) {
		    		room.setLastContent(chatMessageDto.getContent());
		    		room.setLastCredt(chatMessageDto.getCredt());
		    	}
		    }
		    
		    // adm 가공
		    List<Participant> participants = room.getAdm();
		    if(participants != null && participants.size() > 1) {
		    	List<Participant> admList = participants.subList(1, participants.size());
		    	room.setAdm(admList);
		    }
		} 
         
		return list;
	}
	
	// 채팅방 관리 디테일 조회
	public Chatroom getChatManageInfo(Chatroom chatroom) {
		String chatroomId = chatroom.getChatroomId();
		return chatroomRepository.findById(chatroomId).get();
	}

	// 채팅방 관리 디테일 저장
	public void saveChatManageInfo(Chatroom chatroom) {
		String credt = Util.getNowDttm();
		Chatroom saveVo = chatroomRepository.findById(chatroom.getChatroomId()).get();
		saveVo.setCategory(chatroom.getCategory());
		saveVo.setMemo(chatroom.getMemo());
		saveVo.setUpddt(credt);
		chatroomRepository.save(saveVo);
	}
	
	// 채팅방 상테 업데이트
	public void closeChatroom(String chatroomId) {
		Chatroom room = chatroomRepository.findById(chatroomId)
				.orElseThrow(() -> new RuntimeException("Chatroom not found"));

		String credt = Util.getNowDttm();
		room.setUpddt(credt);
		room.setStatus("03");
		chatroomRepository.save(room);
	}
	
	// 참여자 업데이트
	public String addParticipant(Chat chatMessageDto) {
		String status = "01";
		Chatroom room = chatroomRepository.findById(chatMessageDto.getChatroomId())
				.orElseThrow(() -> new RuntimeException("Chatroom not found"));

		// 참여자 build
		Participant participant = Participant.builder().id(chatMessageDto.getId()).nick(chatMessageDto.getNick())
				.joindt(chatMessageDto.getCredt()) // 현재 날짜와 시간으로 joindt 설정
				.build();
		
		List<Participant> participants = new ArrayList<Participant>();
		
		
		if (room.getParticipants() != null) {
			participants = room.getParticipants();
			status = "02"; // 진행중
		}else {
			try {
				// firebase type01 알람
				FcmPush msg = FcmPush.builder()
						.type("type01")
						.title("[실시간 채팅 지원 요청]")
						.cont(chatMessageDto.getContent())
						.chatroomId(chatMessageDto.getChatroomId())
						.sender(chatMessageDto.getId())
						.build();
				
				fcmService.sendNotification(msg);
			} catch (Exception e) {
				e.printStackTrace();
				log.debug("알림 실패");
			}
			
		}
		
		
		participants.add(participant);
		room.setParticipants(participants);

		room.setStatus(status);

		// chatroom에 채팅방 등록
		chatroomRepository.save(room);
		
		return status;
	}

	public boolean isNewParticipant(Chat chat) {
		Query query = new Query(
			    Criteria.where("_id").is(chat.getChatroomId())
			            .and("participants")
			            .elemMatch(Criteria.where("_id").is(chat.getId()))
			);

		boolean exists = mongoTemplate.exists(query, "chatrooms");
		boolean isNew = !exists;
		return isNew;
	}
	
}
