package com.civicpulse.modules.discussion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProfanityFilterService {

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "spam", "scam", "phishing", "hate", "abuse", "malware"
    );

    public boolean containsInappropriateContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String lower = content.toLowerCase();
        for (String keyword : BLOCKED_KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lower).find()) {
                log.warn("Detected blocked keyword [{}] in discussion content.", keyword);
                return true;
            }
        }
        return false;
    }
}
