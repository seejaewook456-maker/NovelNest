import type { EpisodeWorkspacePanelKey } from './EpisodeToolRail';
import CharacterReferencePanel from './CharacterReferencePanel';
import WorldSettingReferencePanel from './WorldSettingReferencePanel';
import AiChatPanel from '../AiChatPanel';

const PANEL_TITLES: Record<EpisodeWorkspacePanelKey, string> = {
  characters: '등장인물 관리',
  worldSettings: '세계관 관리',
  chat: 'AI 채팅',
};

interface EpisodeSidePanelProps {
  novelId: number;
  activePanel: EpisodeWorkspacePanelKey | null;
  // 한 번이라도 열었던 패널 — 계속 마운트 상태를 유지해 검색어/스크롤/대화 내용을 보존한다.
  visited: Set<EpisodeWorkspacePanelKey>;
  onClose: () => void;
}

// 오른쪽에서 슬라이드로 열리는 작업 패널. 폭 애니메이션(width)과 슬라이드 애니메이션(transform)을
// 함께 적용해 "펼쳐지며 오른쪽에서 들어오는" 느낌을 준다. 패널 전환(activePanel 변경)은
// 이 열림/닫힘 애니메이션과 무관하게 내부 콘텐츠만 즉시 바뀐다.
export default function EpisodeSidePanel({ novelId, activePanel, visited, onClose }: EpisodeSidePanelProps) {
  const isOpen = activePanel !== null;

  return (
    <div className={`episode-workspace-panel-wrap${isOpen ? ' open' : ''}`}>
      <div className="episode-workspace-panel-backdrop" onClick={onClose} />
      <div className="episode-workspace-panel">
        <div className="episode-workspace-panel-header">
          <h3>{activePanel ? PANEL_TITLES[activePanel] : ''}</h3>
          <button
            type="button"
            className="episode-workspace-panel-close"
            onClick={onClose}
            aria-label="패널 닫기"
          >
            ✕
          </button>
        </div>
        <div className="episode-workspace-panel-body">
          {/* 한 번 열린 패널은 계속 마운트해두고 display로만 보이기/숨기기를 전환한다.
              — 처음 열 때만 데이터를 조회하고, 닫았다 다시 열어도 상태가 유지된다. */}
          {visited.has('characters') && (
            <div
              className="episode-workspace-panel-slot"
              style={{ display: activePanel === 'characters' ? 'flex' : 'none' }}
            >
              <CharacterReferencePanel novelId={novelId} />
            </div>
          )}
          {visited.has('worldSettings') && (
            <div
              className="episode-workspace-panel-slot"
              style={{ display: activePanel === 'worldSettings' ? 'flex' : 'none' }}
            >
              <WorldSettingReferencePanel novelId={novelId} />
            </div>
          )}
          {visited.has('chat') && (
            <div
              className="episode-workspace-panel-slot"
              style={{ display: activePanel === 'chat' ? 'flex' : 'none' }}
            >
              <AiChatPanel novelId={novelId} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
