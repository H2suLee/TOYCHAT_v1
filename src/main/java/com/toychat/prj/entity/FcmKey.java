package com.toychat.prj.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "fcmKey")
public class FcmKey {
	
	public static final String SEQUENCE_NAME = "fcmKey_sequence";
	
    @Id
	private String id;
	private String fcmKey;

}	
