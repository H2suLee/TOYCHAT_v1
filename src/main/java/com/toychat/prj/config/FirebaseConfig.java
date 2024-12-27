package com.toychat.prj.config;

import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void initializeFirebase() {
        try {
            FileInputStream serviceAccount =
                new FileInputStream("src/main/resources/firebase/toychat-1a2b7-firebase-adminsdk-r1din-72d29b19c2.json");

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase Admin SDK initialized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
