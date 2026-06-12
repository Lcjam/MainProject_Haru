package com.example.demo.daoimpl;

import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageDAOImpl implements ChatMessageDAO {
    private final SqlSession sqlSession;
    private static final String NAMESPACE = "com.example.demo.mapper.ChatMessageMapper.";

    @Override
    public void saveMessage(ChatMessage message) {
        sqlSession.insert(NAMESPACE + "saveMessage", message);
    }

    @Override
    public List<ChatMessage> getMessagesBySessionId(String sessionId) {
        return sqlSession.selectList(NAMESPACE + "getMessagesBySessionId", sessionId);
    }
} 