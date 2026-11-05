package org.example.global.ai.util;

import java.util.ArrayDeque;
import java.util.Deque;

// AI가 반환한 JSON의 괄호 짝이 맞지 않을 때(예: 객체를 닫은 뒤 '}'를 하나 더 쓰는 경우) 복구하는 유틸.
//
// 문자열 값("...") 안의 내용은 절대 건드리지 않고, 문자열 밖의 { } [ ] 짝만 맞춘다.
// - 짝이 없는 닫는 괄호 → 제거           예) [{"a":1}}]       → [{"a":1}]
// - 안쪽 괄호를 닫지 않고 바깥을 닫음 → 빠진 닫는 괄호 보충  예) [{"a":["x"}]  → [{"a":["x"]}]
// - 끝까지 닫히지 않은 괄호 → 끝에 보충
// 파싱이 한 번 실패했을 때만 호출하는 "예비 경로"이므로, 정상 응답에는 영향이 없다.
public final class AiJsonRepair {

    private AiJsonRepair() {
    }

    public static String balanceBrackets(String json) {
        if (json == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(json.length());
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // 문자열 안: 이스케이프(\")만 추적하고 그대로 복사
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '{' || c == '[') {
                stack.push(c);
                out.append(c);
            } else if (c == '}' || c == ']') {
                char expectedOpen = (c == '}') ? '{' : '[';
                if (!stack.isEmpty() && stack.peek() == expectedOpen) {
                    stack.pop();
                    out.append(c);
                } else if (stack.contains(expectedOpen)) {
                    // 안쪽 괄호가 덜 닫힌 상태 — 빠진 닫는 괄호를 먼저 채운다
                    while (stack.peek() != expectedOpen) {
                        out.append(closerOf(stack.pop()));
                    }
                    stack.pop();
                    out.append(c);
                }
                // 그 외(짝이 되는 여는 괄호가 아예 없음)는 불필요한 닫는 괄호이므로 버린다
            } else {
                out.append(c);
            }
        }

        if (inString) {
            out.append('"');
        }
        while (!stack.isEmpty()) {
            out.append(closerOf(stack.pop()));
        }
        return out.toString();
    }

    private static char closerOf(char open) {
        return open == '{' ? '}' : ']';
    }
}
