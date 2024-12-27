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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.toychat.prj.common.sequence.SequenceService;
import com.toychat.prj.common.util.Util;
import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.Chatroom;
import com.toychat.prj.entity.ChatroomInfo;
import com.toychat.prj.entity.Participant;
import com.toychat.prj.entity.User;
import com.toychat.prj.repository.ChatroomRepository;
import com.toychat.prj.repository.UserRepository;

import jakarta.annotation.Resource;

@Service
public class ChatroomService {

	private final MongoTemplate mongoTemplate;

	@Autowired
    public ChatroomService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

	@Autowired
	private SequenceService sequenceService;

	@Autowired
	private ChatroomRepository chatroomRepository;
	
    @Autowired
    private UserRepository userRepository;

	@Resource(name = "Util")
    private Util util;
	
	// 채팅방 생성
	public Chatroom createRoom(User user) {
		 // roomId uuid 생성
		 String roomId = String.valueOf(sequenceService.generateSequence(Chatroom.SEQUENCE_NAME));
		 // credt YYYY-MM-DD HH24:NN:DD 생성
        String credt = util.getNowDttm();

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

	// 채팅방 리스트 by userid
	// roomId, credt, 내용, 상담원, 상태, 마지막 메시지의 chatId, 마지막 메시지의 credt 
	public List<ChatroomInfo> getChatRoomsByUserId(User user) {
		String userId = user.getId();
		
        // participants._id = userId
        MatchOperation matchOperation = match(Criteria.where("participants._id").is(userId).and("participants").ne(null));

		// projection
		ProjectionOperation addParticipantsSizeProjection = project()
		    .and("_id").as("_id")
		    //.and(ArrayOperators.ArrayElemAt.arrayOf("participants").elementAt(0)).as("usr")
		    .and(ArrayOperators.ArrayElemAt.arrayOf("participants").elementAt(1)).as("adm")
		    .and("status").as("status")
		    .and("credt").as("credt")
		    .and(ArrayOperators.Size.lengthOfArray(
		        ConditionalOperators.ifNull("participants").then(new ArrayList<>())
		    )).as("participantsSize");        

        MatchOperation filterByParticipantsSize = match(Criteria.where("participantsSize").gte(2));

        // Lookup을 사용하여 "chats" 컬렉션에서 데이터 조회
        LookupOperation lookupOperation = lookup("chats", "_id", "chatroomId", "lastMessages");

        // lastMessages 배열을 unwind 하여 각 채팅방의 마지막 메시지를 가져옵니다.
        UnwindOperation unwindOperation = unwind("lastMessages", true);

        // credt 내림차순
        SortOperation sortOperation = sort(Sort.by(Sort.Direction.DESC, "lastMessages.credt"));

        // 필요 필드 그룹화
        GroupOperation groupOperation = group("_id")
                .first("_id").as("chatroomId")
        	    //.first("usr").as("usr")
        	    .first("adm").as("adm")
                .first("credt").as("credt")
                .first("status").as("status")
                .first("lastMessages.content").as("lastContent")
                .first("lastMessages.type").as("lastChatType")
                .first("lastMessages.credt").as("lastCredt");
        
        // 상태, 마지막 채팅일시 내림차순
        SortOperation finalSortOperation = sort(Sort.by(Sort.Order.asc("status"),Sort.Order.desc("lastCredt")));

        // Aggregation 파이프라인을 설정
        Aggregation aggregation = newAggregation(
                matchOperation,
                addParticipantsSizeProjection,
                filterByParticipantsSize,
                lookupOperation,
                unwindOperation,
                sortOperation,
                groupOperation,
                finalSortOperation
        );

        // Aggregation 실행
        AggregationResults<ChatroomInfo> results = mongoTemplate.aggregate(aggregation, "chatrooms", ChatroomInfo.class);
        List<ChatroomInfo> list = results.getMappedResults();
        
        return list;
	}

	// 채팅 관리 리스트 : 상태가 02, 03 인 [카테고리, 상태(진행중/완료), 채팅방 생성일, 채팅방 수정일, 문의자, 관리(메모)]
	public List<ChatroomInfo> getChatRoomsMngList(HashMap<String, Object> searchMap) {
		// 필터링 조건
		MatchOperation matchOperation = match(Criteria.where("status").is("03")
		                                             .and("participants").ne(null));

		// participants의 수 계산 및 필터링
		ProjectionOperation addParticipantsSizeProjection = project()
		    .and("_id").as("chatroomId")
		    .and(ArrayOperators.ArrayElemAt.arrayOf("participants").elementAt(0)).as("usr")
		    .and(ArrayOperators.ArrayElemAt.arrayOf("participants").elementAt(1)).as("adm")
		    .and("status").as("status")
		    .and("credt").as("credt")
		    .and("upddt").as("upddt")
		    .and(ArrayOperators.Size.lengthOfArray(
		        ConditionalOperators.ifNull("participants").then(new ArrayList<>())
		    )).as("participantsSize");

		MatchOperation filterByParticipantsSize = match(Criteria.where("participantsSize").gte(2));

		// 그룹화 및 정렬
		GroupOperation groupOperation = group("_id")
		    .first("_id").as("chatroomId")
		    .first("usr").as("usr")
		    .first("adm").as("adm")
		    .first("status").as("status")
		    .first("credt").as("credt")
		    .first("upddt").as("upddt");

		SortOperation finalSortOperation = sort(Sort.by(Sort.Direction.DESC, "credt"));

		// Aggregation 파이프라인을 설정
		Aggregation aggregation = newAggregation(
		    matchOperation,
		    addParticipantsSizeProjection,
		    filterByParticipantsSize,
		    groupOperation,
		    finalSortOperation
		);

		// Aggregation 실행
		AggregationResults<ChatroomInfo> results = mongoTemplate.aggregate(aggregation, "chatrooms", ChatroomInfo.class);
		List<ChatroomInfo> list = results.getMappedResults();
        return list;
	}

	// 실시간 상담 대기 리스트 : 상태가 01 인 [생성일, 사용자 이름, 상태]
	public List<ChatroomInfo> getLiveChatWaitingList(HashMap<String, Object> searchMap) {
        // 쿼리 작성
        Query query = new Query()
                .addCriteria(Criteria.where("status").in("01", "02"))
                .addCriteria(Criteria.where("participants").ne(null));

        query.fields()
             .include("credt")
             .include("status")
             .include("participants.id")
             .include("participants.nick")
             .include("_id");

        // 쿼리 실행
        List<ChatroomInfo> results = mongoTemplate.find(query, ChatroomInfo.class, "chatrooms");
        return results;
	}

	// 채팅방 관리 디테일 조회
	public Chatroom getChatManageInfo(Chatroom chatroom) {
		String chatroomId = chatroom.getChatroomId();
		return chatroomRepository.findById(chatroomId).get();
	}

	// 채팅방 관리 디테일 저장
	public void saveChatManageInfo(Chatroom chatroom) {
		String credt = util.getNowDttm();
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

		String credt = util.getNowDttm();
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
		System.out.println("exists : " + exists);
		boolean isNew = !exists;
		return isNew;
	}
	
}
