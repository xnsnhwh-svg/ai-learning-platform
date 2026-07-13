package com.exam.service.agent.model;

import com.exam.entity.UserProfile;

public class ProfileContext extends AgentContext {
    private final UserProfile existingProfile;

    public ProfileContext(Long userId, Long sessionId, String userMessage,
                          java.util.List<java.util.Map<String, String>> conversationHistory,
                          UserProfile existingProfile) {
        super(userId, sessionId, userMessage, conversationHistory);
        this.existingProfile = existingProfile;
    }

    public boolean hasExistingProfile() { return existingProfile != null && existingProfile.getDimensions() != null; }
    public UserProfile getExistingProfile() { return existingProfile; }
}
