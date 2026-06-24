import { useState, useEffect, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getWorldSettings, createWorldSetting, updateWorldSetting, deleteWorldSetting } from '../api/worldSettingApi';
import type { WorldSetting, WorldSettingCreateRequest, WorldSettingCategory } from '../types/worldsetting';
import { CATEGORY_LABELS } from '../types/worldsetting';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

const CATEGORIES: WorldSettingCategory[] = [
  'COUNTRY', 'RACE', 'MAGIC', 'ORGANIZATION', 'PLACE', 'EVENT', 'ITEM', 'RULE', 'ETC',
];

export default function WorldSettingPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [settings, setSettings] = useState<WorldSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [newCategory, setNewCategory] = useState<WorldSettingCategory>('ETC');
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [adding, setAdding] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editCategory, setEditCategory] = useState<WorldSettingCategory>('ETC');
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!novelId) return;
    getWorldSettings(Number(novelId))
      .then(setSettings)
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    setAdding(true);
    try {
      const body: WorldSettingCreateRequest = { category: newCategory, title: newTitle, content: newContent };
      const created = await createWorldSetting(Number(novelId), body);
      setSettings((prev) => [...prev, created]);
      setNewCategory('ETC'); setNewTitle(''); setNewContent('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '설정 추가 실패');
    } finally {
      setAdding(false);
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
      setSettings((prev) => prev.map((s) => (s.id === settingId ? updated : s)));
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

  if (loading) return <LoadingSpinner />;

  return (
    <div className="manage-page">
      <BackLink label="← 작품으로" onClick={() => navigate(`/novels/${novelId}`)} />
      <PageHeader title="세계관 설정 관리" />

      <div className="add-form-box">
        <h3>새 설정 추가</h3>
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
          <Button type="submit" variant="primary" disabled={adding}>
            {adding ? '추가 중...' : '설정 추가'}
          </Button>
        </form>
      </div>

      {settings.length === 0 ? (
        <EmptyState message="등록된 세계관 설정이 없습니다." />
      ) : (
        settings.map((s) => (
          <div key={s.id} className="item-card">
            <div className="item-card-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span className="category-badge">{CATEGORY_LABELS[s.category]}</span>
                <h3>{s.title}</h3>
              </div>
              <div className="item-card-actions">
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
        ))
      )}
    </div>
  );
}
