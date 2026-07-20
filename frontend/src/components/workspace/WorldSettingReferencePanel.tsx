import { useEffect, useMemo, useState } from 'react';
import { getWorldSettings, toggleWorldSettingFavorite } from '../../api/worldSettingApi';
import type { WorldSetting, WorldSettingCategory } from '../../types/worldsetting';
import { CATEGORY_LABELS } from '../../types/worldsetting';
import LoadingSpinner from '../LoadingSpinner';
import EmptyState from '../EmptyState';

interface WorldSettingReferencePanelProps {
  novelId: number;
}

// category ASC → isFavorite DESC → title ASC 정렬 (WorldSettingPage와 동일한 규칙)
function sortSettings(list: WorldSetting[]): WorldSetting[] {
  return [...list].sort((a, b) => {
    if (a.category !== b.category) return a.category.localeCompare(b.category);
    if (a.isFavorite !== b.isFavorite) return b.isFavorite ? 1 : -1;
    return a.title.localeCompare(b.title);
  });
}

// 회차 작성 중 참고용으로 세계관 설정을 보여주는 패널.
// 새로운 CRUD를 추가하지 않고 기존 조회/즐겨찾기 API만 재사용하며,
// 전체 편집(추가/수정/삭제)은 기존 관리 페이지를 새 탭으로 열어 그대로 사용한다.
export default function WorldSettingReferencePanel({ novelId }: WorldSettingReferencePanelProps) {
  const [settings, setSettings] = useState<WorldSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<WorldSettingCategory | 'ALL'>('ALL');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    getWorldSettings(novelId)
      .then((data) => setSettings(sortSettings(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const handleToggleFavorite = async (id: number, current: boolean) => {
    try {
      const updated = await toggleWorldSettingFavorite(id, !current);
      setSettings((prev) => sortSettings(prev.map((s) => (s.id === id ? updated : s))));
    } catch {
      // 참고용 패널이므로 실패해도 조용히 무시하고 기존 목록 상태를 유지한다.
    }
  };

  const activeCategories = useMemo(() => {
    const seen = new Set<WorldSettingCategory>();
    const ordered: WorldSettingCategory[] = [];
    for (const s of settings) {
      if (!seen.has(s.category)) {
        seen.add(s.category);
        ordered.push(s.category);
      }
    }
    return ordered;
  }, [settings]);

  const keyword = search.trim().toLowerCase();
  const filtered = settings.filter((s) => {
    if (categoryFilter !== 'ALL' && s.category !== categoryFilter) return false;
    if (keyword && !s.title.toLowerCase().includes(keyword)) return false;
    return true;
  });

  return (
    <div className="workspace-ref-panel">
      <div className="workspace-ref-panel-toolbar">
        <input
          type="text"
          className="workspace-ref-search"
          placeholder="제목 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <a
          className="workspace-ref-manage-link"
          href={`/novels/${novelId}/world-settings`}
          target="_blank"
          rel="noopener noreferrer"
        >
          전체 관리 ↗
        </a>
      </div>

      {activeCategories.length > 0 && (
        <div className="workspace-ref-filter-chips">
          <button
            type="button"
            className={`workspace-ref-filter-chip${categoryFilter === 'ALL' ? ' active' : ''}`}
            onClick={() => setCategoryFilter('ALL')}
          >
            전체
          </button>
          {activeCategories.map((cat) => (
            <button
              key={cat}
              type="button"
              className={`workspace-ref-filter-chip${categoryFilter === cat ? ' active' : ''}`}
              onClick={() => setCategoryFilter(cat)}
            >
              {CATEGORY_LABELS[cat]}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <p className="error-message">{error}</p>
      ) : filtered.length === 0 ? (
        <EmptyState message={settings.length === 0 ? '등록된 세계관 설정이 없습니다.' : '검색 결과가 없습니다.'} />
      ) : (
        filtered.map((s) => {
          const isExpanded = expandedId === s.id;
          return (
            <div key={s.id} className="item-card workspace-ref-card">
              <div
                className="item-card-header workspace-ref-card-header"
                role="button"
                tabIndex={0}
                onClick={() => setExpandedId(isExpanded ? null : s.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') setExpandedId(isExpanded ? null : s.id);
                }}
              >
                <h3>
                  <span className="category-badge">{CATEGORY_LABELS[s.category]}</span>{' '}
                  {s.title}
                </h3>
                <button
                  type="button"
                  className={`favorite-btn${s.isFavorite ? ' favorited' : ''}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    void handleToggleFavorite(s.id, s.isFavorite);
                  }}
                  title={s.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                >
                  {s.isFavorite ? '★' : '☆'}
                </button>
              </div>
              {isExpanded && <p className="item-field workspace-ref-card-detail">{s.content}</p>}
            </div>
          );
        })
      )}
    </div>
  );
}
