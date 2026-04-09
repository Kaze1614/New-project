package com.datong.mathai.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockAIProvider implements AIProvider {

    @Override
    public MistakeAnalysisPayload analyze(String title, String content) {
        String source = (title + " " + content).toLowerCase();
        boolean isLimit = source.contains("\u6781\u9650") || source.contains("limit");
        boolean isDerivative = source.contains("\u5bfc\u6570") || source.contains("derivative");

        String errorType = isLimit
            ? "Concept confusion"
            : (isDerivative ? "Formula misuse" : "Step omission");

        List<String> knowledgePoints = new ArrayList<>(List.of(
            "Understand the problem and extract constraints",
            "Select the right core formula",
            "Write a complete and checkable solution"
        ));
        if (isLimit) {
            knowledgePoints.add("Limit existence and continuity conditions");
        }
        if (isDerivative) {
            knowledgePoints.add("Derivative definition and common differentiation rules");
        }

        return new MistakeAnalysisPayload(
            knowledgePoints,
            errorType,
            List.of(
                "Step 1: identify known conditions and target quantity.",
                "Step 2: choose method (definition, formula, or transformation).",
                "Step 3: derive line by line with explicit justification.",
                "Step 4: verify boundary conditions and result reasonableness."
            ),
            List.of(
                "1 basic consolidation problem in the same chapter",
                "1 medium variant problem",
                "1 mixed application problem"
            ),
            List.of(
                "Restate your core idea in one sentence before solving.",
                "Compare your wrong step with the standard step side by side.",
                "Write why you selected this method before starting calculations."
            )
        );
    }

    @Override
    public ChatReplyPayload reply(String message, List<String> contextMessages) {
        String answer = "This is an AI mock response.\n"
            + "1) Extract known conditions and goal;\n"
            + "2) Pick the best mathematical method;\n"
            + "3) Derive step by step with reasons;\n"
            + "4) Verify the final result.\n"
            + "Your question: \"" + message + "\". "
            + "Please share your current attempt, and I will correct it step by step.";

        return new ChatReplyPayload(answer, List.of(
            "What are the known conditions?",
            "Which method are you planning to use?",
            "Send your full steps and I will review each one."
        ));
    }
}