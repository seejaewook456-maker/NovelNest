import { useState, type ReactNode, type CSSProperties } from 'react';
import EpisodeToolRail, { type EpisodeWorkspacePanelKey } from './EpisodeToolRail';
import EpisodeSidePanel from './EpisodeSidePanel';

interface EpisodeWorkspaceProps {
  novelId: number;
  children: ReactNode;
  // 회차 작성 입력 박스처럼, 패널이 열려도 절대 축소되면 안 되는 고정 폭 콘텐츠일 때 지정한다.
  // 지정하지 않으면 기존처럼 유연한 폭(flex:1)을 사용한다(회차 읽기 전용 화면 등).
  fixedContentWidth?: number;
}

// 회차 작성/수정 화면에 "메뉴 사이드바 + 작업 패널"을 덧붙이는 레이아웃 래퍼.
// 기존 페이지의 내용은 children으로 그대로 감싸기만 하므로, 회차 작성/수정/자동 저장 등
// 기존 로직은 전혀 건드리지 않는다.
//
// children에는 EditorHeader(뒤로가기+제목+저장 상태) + 입력 박스(Card)를 함께 넘긴다 —
// 같은 flex 컬럼 안에 두어야 패널 열림/닫힘·화면 크기 변경과 무관하게 둘이 항상 같은 위치를
// 유지한다. 대신 메뉴 레일·패널의 상단 테두리를 입력 박스(EditorHeader 아래)와 맞추기 위해,
// fixedContentWidth가 있을 때(=EditorHeader가 있는 회차 작성/수정 화면)만 has-header 클래스를
// 붙여 CSS가 헤더 높이만큼 메뉴/패널을 아래로 내리도록 한다.
export default function EpisodeWorkspace({ novelId, children, fixedContentWidth }: EpisodeWorkspaceProps) {
  const [activePanel, setActivePanel] = useState<EpisodeWorkspacePanelKey | null>(null);
  // 한 번이라도 연 패널만 마운트 목록에 추가한다 — 패널을 열기 전에는
  // 등장인물/세계관/AI 채팅 데이터를 전혀 조회하지 않기 위함.
  const [visited, setVisited] = useState<Set<EpisodeWorkspacePanelKey>>(new Set());

  const handleSelect = (panel: EpisodeWorkspacePanelKey) => {
    setActivePanel((prev) => (prev === panel ? null : panel));
    setVisited((prev) => (prev.has(panel) ? prev : new Set(prev).add(panel)));
  };

  const handleClose = () => setActivePanel(null);

  return (
    <div
      className={`episode-workspace${activePanel ? ' panel-open' : ''}${fixedContentWidth ? ' has-header' : ''}`}
    >
      <div
        className={`episode-workspace-main${fixedContentWidth ? ' fixed-width' : ''}`}
        // 인라인 width 대신 CSS 커스텀 프로퍼티로 넘겨서, 모바일 미디어쿼리가
        // .fixed-width의 width를 100%로 재정의할 수 있게 한다(인라인 스타일은 !important 없이
        // 스타일시트 규칙으로 덮어쓸 수 없어 가로 스크롤을 유발하기 때문).
        style={
          fixedContentWidth
            ? ({ '--ep-fixed-width': `${fixedContentWidth}px` } as CSSProperties)
            : undefined
        }
      >
        {children}
      </div>

      <EpisodeSidePanel
        novelId={novelId}
        activePanel={activePanel}
        visited={visited}
        onClose={handleClose}
      />

      <EpisodeToolRail activePanel={activePanel} onSelect={handleSelect} />
    </div>
  );
}
