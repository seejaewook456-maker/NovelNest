// 회차 본문 글자 수 계산 — 작성/수정 화면(WritingAssistToolbar)과 회차 상세 화면이 같은 기준을 쓰도록 공통으로 둔다.
// 기준:
//  - 공백 포함: 문자열 길이 그대로(공백·줄바꿈 모두 1자로 셈)
//  - 공백 제외: 공백/탭/줄바꿈 등 모든 공백 문자(\s)를 뺀 길이
export interface CharCount {
  withSpaces: number;
  withoutSpaces: number;
}

// 본문이 비어 있거나 없으면(null/undefined) 0자로 계산한다.
export function countChars(content: string | null | undefined): CharCount {
  const text = content ?? '';
  return {
    withSpaces: text.length,
    withoutSpaces: text.replace(/\s/g, '').length,
  };
}
