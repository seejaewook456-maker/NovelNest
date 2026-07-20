import type { ReactNode } from 'react';
import BackLink from '../BackLink';

interface EditorHeaderProps {
  backLabel: string;
  onBack: () => void;
  title: string;
  statusBadge?: ReactNode;
}

// 회차 작성/수정 화면의 "뒤로가기 + 제목 + 저장 상태" 헤더.
// 반드시 EpisodeWorkspace의 children(= 입력 박스와 같은 flex 컬럼) 안에서 렌더링해야 한다.
// 그래야 패널 열림/닫힘, 브라우저 크기 변경, breakout 레이아웃 전환 등 어떤 상황에서도
// 입력 박스와 완전히 같은 위치 계산을 공유해 서로 어긋나지 않는다.
export default function EditorHeader({ backLabel, onBack, title, statusBadge }: EditorHeaderProps) {
  return (
    <div className="episode-editor-header">
      <div className="episode-editor-header-left">
        <BackLink label={backLabel} onClick={onBack} />
        <h2>{title}</h2>
      </div>
      {statusBadge && <div className="episode-editor-header-right">{statusBadge}</div>}
    </div>
  );
}
