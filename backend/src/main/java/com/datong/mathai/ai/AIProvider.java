package com.datong.mathai.ai;

import java.util.List;

public interface AIProvider {
    MistakeAnalysisPayload analyze(String title, String content);

    ChatReplyPayload reply(String message, List<String> contextMessages);
}
