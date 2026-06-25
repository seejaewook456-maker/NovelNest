import type { RefObject } from 'react';

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
  textareaRef: RefObject<HTMLTextAreaElement>;
}

export default function WritingAssistToolbar({ content, onChange, textareaRef }: Props) {
  const withoutSpaces = content.replace(/\s/g, '').length;
  const withSpaces = content.length;

  // 커서 위치에 텍스트 삽입, 선택 영역 있으면 대체
  const insertText = (insert: string, cursorOffset: number) => {
    const ta = textareaRef.current;
    if (!ta) return;

    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const newValue = content.slice(0, start) + insert + content.slice(end);
    const newCursor = start + cursorOffset;

    onChange(newValue);
    requestAnimationFrame(() => {
      ta.focus();
      ta.setSelectionRange(newCursor, newCursor);
    });
  };

  // 앞뒤 줄바꿈을 고려해 구분선 삽입
  const insertDivider = () => {
    const ta = textareaRef.current;
    if (!ta) return;

    const pos = ta.selectionStart;
    const before = content.slice(0, pos);
    const after = content.slice(pos);

    const prefix = before.length > 0 && !before.endsWith('\n') ? '\n' : '';
    const suffix = after.length > 0 && !after.startsWith('\n') ? '\n' : '';
    const insert = `${prefix}${DIVIDER}${suffix}`;
    const newValue = before + insert + after;
    const newPos = pos + insert.length;

    onChange(newValue);
    requestAnimationFrame(() => {
      ta.focus();
      ta.setSelectionRange(newPos, newPos);
    });
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
