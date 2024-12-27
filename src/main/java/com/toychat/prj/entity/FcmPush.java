package com.toychat.prj.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "fcmPush")
public class FcmPush {
	
	public static final String SEQUENCE_NAME = "fcmPush_sequence";
	
	@Id
	private String seq;
	private String target;
	private String title;
	private String cont;
	private String chkYn;
	private String chkDt;
	
	private String token;
}
