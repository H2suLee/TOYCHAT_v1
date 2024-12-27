package com.toychat.prj.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.toychat.prj.entity.Chat;
import com.toychat.prj.entity.FcmPush;

public interface FcmPushRepository extends MongoRepository<FcmPush, String>{

}
