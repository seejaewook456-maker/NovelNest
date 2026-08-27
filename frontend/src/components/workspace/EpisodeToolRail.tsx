import type { ReactNode } from 'react';

export type EpisodeWorkspacePanelKey = 'memos' | 'previousEpisodes' | 'characters' | 'worldSettings' | 'chat';

interface MenuItem {
  key: EpisodeWorkspacePanelKey;
  label: string;
  icon: ReactNode;
}

// 이모지 대신 CDN 의존 없는 인라인 SVG 라인 아이콘을 사용해 OS/브라우저마다 다르게 보이지 않고
// 항상 같은 모습으로, 브랜드 색(currentColor)을 그대로 상속받아 보이도록 한다.
const MemoIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
    <polyline points="14 2 14 8 20 8" />
    <line x1="8" y1="13" x2="16" y2="13" />
    <line x1="8" y1="17" x2="16" y2="17" />
  </svg>
);

// 열린 책 형태 — "이전 회차"(과거 기록을 되짚어보는 회차 목록)를 나타낸다.
const PreviousEpisodesIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2 4.5A2.5 2.5 0 0 1 4.5 2H9a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
    <path d="M22 4.5A2.5 2.5 0 0 0 19.5 2H15a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h8z" />
  </svg>
);

const CharacterIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21v-1a8 8 0 0 1 16 0v1" />
  </svg>
);

const WorldviewIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="12" cy="12" r="10" />
    <line x1="2" y1="12" x2="22" y2="12" />
    <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
  </svg>
);

const ChatIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
  </svg>
);

const MENU_ITEMS: MenuItem[] = [
  { key: 'memos', label: '메모', icon: <MemoIcon /> },
  { key: 'previousEpisodes', label: '이전 회차', icon: <PreviousEpisodesIcon /> },
  { key: 'characters', label: '등장인물', icon: <CharacterIcon /> },
  { key: 'worldSettings', label: '세계관', icon: <WorldviewIcon /> },
  { key: 'chat', label: 'AI 채팅', icon: <ChatIcon /> },
];

interface EpisodeToolRailProps {
  activePanel: EpisodeWorkspacePanelKey | null;
  onSelect: (panel: EpisodeWorkspacePanelKey) => void;
}

// 회차 작성/수정 화면 오른쪽에 항상 떠 있는 작은 메뉴 레일.
// 패널이 열려 있어도 계속 보이므로, 다른 메뉴를 누르면 패널 내용만 교체된다.
export default function EpisodeToolRail({ activePanel, onSelect }: EpisodeToolRailProps) {
  return (
    <nav className="episode-tool-rail" aria-label="회차 작성 도구 메뉴">
      <span className="episode-tool-rail-title">☰ 메뉴</span>
      {MENU_ITEMS.map((item) => (
        <button
          key={item.key}
          type="button"
          className={`episode-tool-rail-btn${activePanel === item.key ? ' active' : ''}`}
          onClick={() => onSelect(item.key)}
          aria-pressed={activePanel === item.key}
        >
          <span className="episode-tool-rail-icon" aria-hidden="true">{item.icon}</span>
          <span className="episode-tool-rail-label">{item.label}</span>
        </button>
      ))}
    </nav>
  );
}
