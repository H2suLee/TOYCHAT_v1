package com.toychat.prj.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.toychat.prj.entity.FcmKey;

public interface FcmKeyRepository extends MongoRepository<FcmKey, String>{

}
