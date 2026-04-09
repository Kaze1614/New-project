package com.datong.mathai.ai;

import java.util.List;

public record ChatReplyPayload(String answer, List<String> suggestions) {
}
