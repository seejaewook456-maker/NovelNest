export type EpisodeWorkspacePanelKey = 'characters' | 'worldSettings' | 'chat';

interface MenuItem {
  key: EpisodeWorkspacePanelKey;
  label: string;
  icon: string;
}

const MENU_ITEMS: MenuItem[] = [
  { key: 'characters', label: '등장인물', icon: '👤' },
  { key: 'worldSettings', label: '세계관', icon: '🌍' },
  { key: 'chat', label: 'AI 채팅', icon: '🤖' },
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
