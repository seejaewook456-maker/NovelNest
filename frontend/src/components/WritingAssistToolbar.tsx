import type { MouseEvent, RefObject } from 'react';

interface SpecialChar {
  label: string;
  insert: string;
  cursor: number; // 삽입 시작점 기준 커서 이동 offset
}

const SPECIAL_CHARS: SpecialChar[] = [
  { label: '……', insert: '……', cursor: 2 },
  { label: '…',  insert: '…',  cursor: 1 },
  { label: '『』', insert: '『』', cursor: 1 },
  { label: '「」', insert: '「」', cursor: 1 },
  { label: '〈〉', insert: '〈〉', cursor: 1 },
  { label: '―',  insert: '―',  cursor: 1 },
];

const DIVIDER = '──────────────';

interface Props {
  content: string;
  onChange: (value: string) => void;
  textareaRef: RefObject<HTMLTextAreaElement | null>;
}

export default function WritingAssistToolbar({ content, onChange, textareaRef }: Props) {
  const withoutSpaces = content.replace(/\s/g, '').length;
  const withSpaces = content.length;

  // 툴바 버튼은 마우스다운 시 기본 동작(포커스 이동)을 막아 textarea의 포커스/선택 영역이
  // 항상 유지되도록 한다. 이렇게 하면 클릭 시점의 selectionStart/End를 안전하게 읽을 수 있다.
  const preventFocusSteal = (e: MouseEvent) => {
    e.preventDefault();
  };

  // controlled textarea의 value를 코드로 갱신하면(React가 DOM value를 직접 설정하면)
  // 브라우저가 scrollTop을 0으로, 커서 위치를 텍스트 끝으로 되돌리는 부작용이 있다.
  // 이를 막기 위해 삽입 전 scrollTop/scrollLeft를 저장해두고, 갱신 후 되돌린다.
  const restoreAfterInsert = (ta: HTMLTextAreaElement, newCursor: number, scrollTop: number, scrollLeft: number) => {
    requestAnimationFrame(() => {
      ta.focus({ preventScroll: true });
      ta.setSelectionRange(newCursor, newCursor);
      ta.scrollTop = scrollTop;
      ta.scrollLeft = scrollLeft;
    });
  };

  // 커서 위치에 텍스트 삽입, 선택 영역 있으면 대체
  const insertText = (insert: string, cursorOffset: number) => {
    const ta = textareaRef.current;
    if (!ta) return;

    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const { scrollTop, scrollLeft } = ta;
    const newValue = content.slice(0, start) + insert + content.slice(end);
    const newCursor = start + cursorOffset;

    onChange(newValue);
    restoreAfterInsert(ta, newCursor, scrollTop, scrollLeft);
  };

  // 앞뒤 줄바꿈을 고려해 구분선 삽입
  const insertDivider = () => {
    const ta = textareaRef.current;
    if (!ta) return;

    const pos = ta.selectionStart;
    const { scrollTop, scrollLeft } = ta;
    const before = content.slice(0, pos);
    const after = content.slice(pos);

    const prefix = before.length > 0 && !before.endsWith('\n') ? '\n' : '';
    const suffix = after.length > 0 && !after.startsWith('\n') ? '\n' : '';
    const insert = `${prefix}${DIVIDER}${suffix}`;
    const newValue = before + insert + after;
    const newPos = pos + insert.length;

    onChange(newValue);
    restoreAfterInsert(ta, newPos, scrollTop, scrollLeft);
  };

  return (
    <div className="writing-toolbar">
      <div className="writing-toolbar-stats">
        <span className="writing-stat">
          공백 제외 <strong>{withoutSpaces.toLocaleString()}</strong>자
        </span>
        <span className="writing-stat-sep">·</span>
        <span className="writing-stat">
          공백 포함 <strong>{withSpaces.toLocaleString()}</strong>자
        </span>
      </div>
      <div className="writing-toolbar-buttons">
        <button
          type="button"
          className="writing-toolbar-btn"
          onMouseDown={preventFocusSteal}
          onClick={insertDivider}
          title="구분선 삽입"
        >
          구분선
        </button>
        <div className="writing-toolbar-sep" />
        {SPECIAL_CHARS.map((sc) => (
          <button
            key={sc.label}
            type="button"
            className="writing-toolbar-btn"
            onMouseDown={preventFocusSteal}
            onClick={() => insertText(sc.insert, sc.cursor)}
            title={sc.insert}
          >
            {sc.label}
          </button>
        ))}
      </div>
    </div>
  );
}
