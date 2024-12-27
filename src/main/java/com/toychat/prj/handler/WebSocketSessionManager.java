package com.toychat.prj.handler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WebSocketSessionManager {
    // 각 타입(chat, admin)에 대해 여러 세션을 관리하는 Map
    private final Map<String, Set<WebSocketSession>> sessionsMap = new ConcurrentHashMap<>();

    // 세션을 추가하는 메서드
    public void addSession(String type, WebSocketSession session) {
        // 해당 타입의 세션 집합을 가져오거나 없으면 새로 생성
        sessionsMap.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    // 특정 타입의 모든 세션을 반환하는 메서드
    public Set<WebSocketSession> getSessions(String type) {
        return sessionsMap.getOrDefault(type, Set.of());
    }

    // 특정 타입에서 세션을 제거하는 메서드
    public void removeSession(String type, WebSocketSession session) {
        Set<WebSocketSession> sessionSet = sessionsMap.get(type);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessionsMap.remove(type);
            }
        }
    }
}