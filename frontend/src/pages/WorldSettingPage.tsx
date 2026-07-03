import { useState, useEffect, useMemo, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getWorldSettings, createWorldSetting, updateWorldSetting, toggleWorldSettingFavorite, deleteWorldSetting } from '../api/worldSettingApi';
import type { WorldSetting, WorldSettingCreateRequest, WorldSettingCategory } from '../types/worldsetting';
import { CATEGORY_LABELS } from '../types/worldsetting';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import CollapsibleFormCard from '../components/CollapsibleFormCard';

const CATEGORIES: WorldSettingCategory[] = [
  'COUNTRY', 'RACE', 'MAGIC', 'ORGANIZATION', 'PLACE', 'EVENT', 'ITEM', 'RULE', 'ETC',
];

// category ASC → isFavorite DESC → title ASC 정렬
function sortSettings(list: WorldSetting[]): WorldSetting[] {
  return [...list].sort((a, b) => {
    if (a.category < b.category) return -1;
    if (a.category > b.category) return 1;
    if (a.isFavorite !== b.isFavorite) return b.isFavorite ? 1 : -1;
    return a.title.localeCompare(b.title);
  });
}

export default function WorldSettingPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [settings, setSettings] = useState<WorldSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const [selectedCategory, setSelectedCategory] = useState<WorldSettingCategory | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [newCategory, setNewCategory] = useState<WorldSettingCategory>('ETC');
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [adding, setAdding] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editCategory, setEditCategory] = useState<WorldSettingCategory>('ETC');
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [saving, setSaving] = useState(false);

  const grouped = useMemo(() => {
    const map = new Map<WorldSettingCategory, WorldSetting[]>();
    for (const s of settings) {
      const list = map.get(s.category) ?? [];
      list.push(s);
      map.set(s.category, list);
    }
    return map;
  }, [settings]);

  const activeCategories = CATEGORIES.filter((cat) => (grouped.get(cat) ?? []).length > 0);
  const selectedSettings = selectedCategory ? (grouped.get(selectedCategory) ?? []) : [];

  useEffect(() => {
    if (!novelId) return;
    getWorldSettings(Number(novelId))
      .then((data) => setSettings(sortSettings(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  useEffect(() => {
    if (selectedCategory !== null) {
      setNewCategory(selectedCategory);
      setNewTitle('');
      setNewContent('');
      setIsFormOpen(false);
    }
  }, [selectedCategory]);

  useEffect(() => {
    if (selectedCategory !== null && (grouped.get(selectedCategory) ?? []).length === 0) {
      setSelectedCategory(null);
    }
  }, [grouped, selectedCategory]);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 2000);
  };

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    setAdding(true);
    try {
      const body: WorldSettingCreateRequest = { category: newCategory, title: newTitle, content: newContent };
      const created = await createWorldSetting(Number(novelId), body);
      setSettings((prev) => sortSettings([...prev, created]));
      setNewTitle('');
      setNewContent('');
      setIsFormOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '설정 추가 실패');
    } finally {
      setAdding(false);
    }
  };

  const handleCancel = () => {
    setNewTitle('');
    setNewContent('');
    setIsFormOpen(false);
  };

  const handleToggleFavorite = async (worldSettingId: number, currentValue: boolean) => {
    try {
      const updated = await toggleWorldSettingFavorite(worldSettingId, !currentValue);
      setSettings((prev) => sortSettings(prev.map((s) => s.id === worldSettingId ? updated : s)));
      showToast(!currentValue ? '즐겨찾기에 추가되었습니다.' : '즐겨찾기가 해제되었습니다.', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : '즐겨찾기 변경에 실패했습니다.', 'error');
    }
  };

  const startEdit = (s: WorldSetting) => {
    setEditingId(s.id);
    setEditCategory(s.category);
    setEditTitle(s.title);
    setEditContent(s.content);
  };

  const handleUpdate = async (settingId: number) => {
    setSaving(true);
    try {
      const updated = await updateWorldSetting(settingId, { category: editCategory, title: editTitle, content: editContent });
      setSettings((prev) => sortSettings(prev.map((s) => s.id === settingId ? updated : s)));
      setEditingId(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (settingId: number) => {
    if (!window.confirm('이 세계관 설정을 삭제하시겠습니까?')) return;
    try {
      await deleteWorldSetting(settingId);
      setSettings((prev) => prev.filter((s) => s.id !== settingId));
    } catch (err) {
      setError(err instanceof Error ? err.message : '삭제 실패');
    }
  };

  const handleBack = () => {
    setSelectedCategory(null);
    setEditingId(null);
    setNewCategory('ETC');
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="manage-page">
      <BackLink label="← 작품으로" onClick={() => navigate(`/novels/${novelId}`)} />
      <PageHeader title="세계관 설정 관리" />

      <CollapsibleFormCard
        label="새 설정 추가"
        isOpen={isFormOpen}
        onToggle={() => setIsFormOpen((v) => !v)}
      >
        <form onSubmit={handleAdd}>
          <div className="form-row">
            <div className="form-group">
              <label>카테고리</label>
              <select value={newCategory} onChange={(e) => setNewCategory(e.target.value as WorldSettingCategory)}>
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>{CATEGORY_LABELS[cat]}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>제목 *</label>
              <input
                type="text"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                placeholder="설정 이름"
                required
              />
            </div>
          </div>
          <div className="form-group">
            <label>내용 *</label>
            <textarea
              value={newContent}
              onChange={(e) => setNewContent(e.target.value)}
              placeholder="세계관 설정 내용"
              rows={4}
              required
            />
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={adding}>
              {adding ? '추가 중...' : '설정 추가'}
            </Button>
            <Button type="button" variant="secondary" onClick={handleCancel} disabled={adding}>
              취소
            </Button>
          </div>
        </form>
      </CollapsibleFormCard>

      {/* 카테고리 목록 뷰 */}
      {selectedCategory === null && (
        settings.length === 0 ? (
          <EmptyState message="아직 등록된 세계관 설정이 없습니다." />
        ) : (
          <div className="category-group-grid">
            {activeCategories.map((cat) => {
              const count = grouped.get(cat)!.length;
              return (
                <div
                  key={cat}
                  className="category-group-card"
                  onClick={() => setSelectedCategory(cat)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && setSelectedCategory(cat)}
                >
                  <div className="category-group-label">{CATEGORY_LABELS[cat]}</div>
                  <div className="category-group-count">{count}개 설정</div>
                </div>
              );
            })}
          </div>
        )
      )}

      {/* 카테고리 상세 뷰 */}
      {selectedCategory !== null && (
        <>
          <div className="category-detail-header">
            <div className="category-detail-title">
              <span className="category-badge">{CATEGORY_LABELS[selectedCategory]}</span>
              <span className="category-detail-count">{selectedSettings.length}개 설정</span>
            </div>
            <button className="category-back-btn" onClick={handleBack} type="button">
              ← 카테고리 목록으로
            </button>
          </div>

          {selectedSettings.map((s) => (
            <div key={s.id} className="item-card">
              <div className="item-card-header">
                <h3>{s.title}</h3>
                <div className="item-card-actions">
                  <button
                    className={`favorite-btn${s.isFavorite ? ' favorited' : ''}`}
                    onClick={() => handleToggleFavorite(s.id, s.isFavorite)}
                    title={s.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                    type="button"
                  >
                    {s.isFavorite ? '★' : '☆'}
                  </button>
                  {editingId !== s.id && (
                    <>
                      <Button variant="secondary" size="sm" onClick={() => startEdit(s)}>수정</Button>
                      <Button variant="danger" size="sm" onClick={() => handleDelete(s.id)}>삭제</Button>
                    </>
                  )}
                </div>
              </div>

              {editingId !== s.id ? (
                <p className="item-field">{s.content}</p>
              ) : (
                <div className="inline-edit-form">
                  <div className="form-row">
                    <div className="form-group">
                      <label>카테고리</label>
                      <select value={editCategory} onChange={(e) => setEditCategory(e.target.value as WorldSettingCategory)}>
                        {CATEGORIES.map((cat) => (
                          <option key={cat} value={cat}>{CATEGORY_LABELS[cat]}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label>제목 *</label>
                      <input type="text" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} required />
                    </div>
                  </div>
                  <div className="form-group">
                    <label>내용 *</label>
                    <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows={4} required />
                  </div>
                  <div className="form-actions">
                    <Button variant="primary" disabled={saving} onClick={() => handleUpdate(s.id)}>
                      {saving ? '저장 중...' : '저장'}
                    </Button>
                    <Button variant="secondary" onClick={() => setEditingId(null)}>취소</Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </>
      )}

      {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}
    </div>
  );
}
