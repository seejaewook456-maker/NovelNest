import { useEffect, useState } from 'react';
import { getCharacters, toggleCharacterFavorite } from '../../api/characterApi';
import type { Character } from '../../types/character';
import LoadingSpinner from '../LoadingSpinner';
import EmptyState from '../EmptyState';

interface CharacterReferencePanelProps {
  novelId: number;
}

// isFavorite DESC → name ASC 정렬 (CharacterPage와 동일한 규칙)
function sortCharacters(list: Character[]): Character[] {
  return [...list].sort((a, b) => {
    if (a.isFavorite !== b.isFavorite) return b.isFavorite ? 1 : -1;
    return a.name.localeCompare(b.name);
  });
}

// 회차 작성 중 참고용으로 등장인물 목록을 보여주는 패널.
// 새로운 CRUD를 추가하지 않고 기존 조회/즐겨찾기 API만 재사용하며,
// 전체 편집(추가/수정/삭제)은 기존 관리 페이지를 새 탭으로 열어 그대로 사용한다.
export default function CharacterReferencePanel({ novelId }: CharacterReferencePanelProps) {
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    getCharacters(novelId)
      .then((data) => setCharacters(sortCharacters(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const handleToggleFavorite = async (id: number, current: boolean) => {
    try {
      const updated = await toggleCharacterFavorite(id, !current);
      setCharacters((prev) => sortCharacters(prev.map((c) => (c.id === id ? updated : c))));
    } catch {
      // 참고용 패널이므로 실패해도 조용히 무시하고 기존 목록 상태를 유지한다.
    }
  };

  const keyword = search.trim().toLowerCase();
  const filtered = keyword
    ? characters.filter((c) => c.name.toLowerCase().includes(keyword))
    : characters;

  return (
    <div className="workspace-ref-panel">
      <div className="workspace-ref-panel-toolbar">
        <input
          type="text"
          className="workspace-ref-search"
          placeholder="이름 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <a
          className="workspace-ref-manage-link"
          href={`/novels/${novelId}/characters`}
          target="_blank"
          rel="noopener noreferrer"
        >
          전체 관리 ↗
        </a>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <p className="error-message">{error}</p>
      ) : filtered.length === 0 ? (
        <EmptyState message={characters.length === 0 ? '등록된 인물이 없습니다.' : '검색 결과가 없습니다.'} />
      ) : (
        filtered.map((c) => {
          const isExpanded = expandedId === c.id;
          const hasDetail = c.age != null || c.personality || c.speechStyle || c.description;
          return (
            <div key={c.id} className="item-card workspace-ref-card">
              <div
                className="item-card-header workspace-ref-card-header"
                role="button"
                tabIndex={0}
                onClick={() => setExpandedId(isExpanded ? null : c.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') setExpandedId(isExpanded ? null : c.id);
                }}
              >
                <h3>
                  {c.name}
                  {c.role && <span className="workspace-ref-card-role"> · {c.role}</span>}
                </h3>
                <button
                  type="button"
                  className={`favorite-btn${c.isFavorite ? ' favorited' : ''}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    void handleToggleFavorite(c.id, c.isFavorite);
                  }}
                  title={c.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                >
                  {c.isFavorite ? '★' : '☆'}
                </button>
              </div>
              {isExpanded && (
                <div className="workspace-ref-card-detail">
                  {c.age != null && <p className="item-field"><span>나이</span>{c.age}세</p>}
                  {c.personality && <p className="item-field"><span>성격</span>{c.personality}</p>}
                  {c.speechStyle && <p className="item-field"><span>말투</span>{c.speechStyle}</p>}
                  {c.description && <p className="item-field"><span>설명</span>{c.description}</p>}
                  {!hasDetail && <p className="item-field">추가 정보가 없습니다.</p>}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}
