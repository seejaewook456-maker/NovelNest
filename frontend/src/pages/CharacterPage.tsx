import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCharacters, createCharacter, updateCharacter, toggleCharacterFavorite, deleteCharacter } from '../api/characterApi';
import type { Character, CharacterCreateRequest } from '../types/character';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import CollapsibleFormCard from '../components/CollapsibleFormCard';

// isFavorite DESC → name ASC 정렬
function sortCharacters(list: Character[]): Character[] {
  return [...list].sort((a, b) => {
    if (a.isFavorite !== b.isFavorite) return b.isFavorite ? 1 : -1;
    return a.name.localeCompare(b.name);
  });
}

export default function CharacterPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [newRole, setNewRole] = useState('');
  const [newAge, setNewAge] = useState('');
  const [newPersonality, setNewPersonality] = useState('');
  const [newSpeechStyle, setNewSpeechStyle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [adding, setAdding] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editRole, setEditRole] = useState('');
  const [editAge, setEditAge] = useState('');
  const [editPersonality, setEditPersonality] = useState('');
  const [editSpeechStyle, setEditSpeechStyle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!novelId) return;
    getCharacters(Number(novelId))
      .then((data) => setCharacters(sortCharacters(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 2000);
  };

  const clearForm = () => {
    setNewName(''); setNewRole(''); setNewAge('');
    setNewPersonality(''); setNewSpeechStyle(''); setNewDescription('');
  };

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    setAdding(true);
    try {
      const body: CharacterCreateRequest = {
        name: newName,
        ...(newRole && { role: newRole }),
        ...(newAge && { age: Number(newAge) }),
        ...(newPersonality && { personality: newPersonality }),
        ...(newSpeechStyle && { speechStyle: newSpeechStyle }),
        ...(newDescription && { description: newDescription }),
      };
      const created = await createCharacter(Number(novelId), body);
      setCharacters((prev) => sortCharacters([...prev, created]));
      clearForm();
      setIsFormOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '인물 추가 실패');
    } finally {
      setAdding(false);
    }
  };

  const handleCancel = () => {
    clearForm();
    setIsFormOpen(false);
  };

  const handleToggleFavorite = async (characterId: number, currentValue: boolean) => {
    try {
      const updated = await toggleCharacterFavorite(characterId, !currentValue);
      setCharacters((prev) => sortCharacters(prev.map((c) => c.id === characterId ? updated : c)));
      showToast(!currentValue ? '즐겨찾기에 추가되었습니다.' : '즐겨찾기가 해제되었습니다.', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : '즐겨찾기 변경에 실패했습니다.', 'error');
    }
  };

  const startEdit = (c: Character) => {
    setEditingId(c.id);
    setEditName(c.name);
    setEditRole(c.role ?? '');
    setEditAge(c.age != null ? String(c.age) : '');
    setEditPersonality(c.personality ?? '');
    setEditSpeechStyle(c.speechStyle ?? '');
    setEditDescription(c.description ?? '');
  };

  const handleUpdate = async (characterId: number) => {
    setSaving(true);
    try {
      const updated = await updateCharacter(characterId, {
        name: editName,
        ...(editRole && { role: editRole }),
        ...(editAge && { age: Number(editAge) }),
        ...(editPersonality && { personality: editPersonality }),
        ...(editSpeechStyle && { speechStyle: editSpeechStyle }),
        ...(editDescription && { description: editDescription }),
      });
      setCharacters((prev) => sortCharacters(prev.map((c) => c.id === characterId ? updated : c)));
      setEditingId(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (characterId: number) => {
    if (!window.confirm('이 인물을 삭제하시겠습니까?')) return;
    try {
      await deleteCharacter(characterId);
      setCharacters((prev) => prev.filter((c) => c.id !== characterId));
    } catch (err) {
      setError(err instanceof Error ? err.message : '삭제 실패');
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="manage-page">
      <BackLink label="← 작품으로" onClick={() => navigate(`/novels/${novelId}`)} />
      <PageHeader title="등장인물 관리" />

      <CollapsibleFormCard
        label="새 인물 추가"
        isOpen={isFormOpen}
        onToggle={() => setIsFormOpen((v) => !v)}
      >
        <form onSubmit={handleAdd}>
          <div className="form-row">
            <div className="form-group">
              <label>이름 *</label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="인물 이름"
                required
              />
            </div>
            <div className="form-group">
              <label>역할</label>
              <input
                type="text"
                value={newRole}
                onChange={(e) => setNewRole(e.target.value)}
                placeholder="주인공, 악당, 조력자 등"
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>나이</label>
              <input
                type="number"
                value={newAge}
                onChange={(e) => setNewAge(e.target.value)}
                placeholder="예) 25"
              />
            </div>
            <div className="form-group">
              <label>말투</label>
              <input
                type="text"
                value={newSpeechStyle}
                onChange={(e) => setNewSpeechStyle(e.target.value)}
                placeholder="예) 격식체, 반말"
              />
            </div>
          </div>
          <div className="form-group">
            <label>성격</label>
            <input
              type="text"
              value={newPersonality}
              onChange={(e) => setNewPersonality(e.target.value)}
              placeholder="예) 용감하고 직설적인"
            />
          </div>
          <div className="form-group">
            <label>설명</label>
            <textarea
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              placeholder="인물에 대한 추가 설명"
              rows={3}
            />
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={adding}>
              {adding ? '추가 중...' : '인물 추가'}
            </Button>
            <Button type="button" variant="secondary" onClick={handleCancel} disabled={adding}>
              취소
            </Button>
          </div>
        </form>
      </CollapsibleFormCard>

      {characters.length === 0 ? (
        <EmptyState message="등록된 인물이 없습니다." />
      ) : (
        characters.map((c) => (
          <div key={c.id} className="item-card">
            <div className="item-card-header">
              <h3>{c.name}</h3>
              <div className="item-card-actions">
                <button
                  className={`favorite-btn${c.isFavorite ? ' favorited' : ''}`}
                  onClick={() => handleToggleFavorite(c.id, c.isFavorite)}
                  title={c.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                  type="button"
                >
                  {c.isFavorite ? '★' : '☆'}
                </button>
                {editingId !== c.id && (
                  <>
                    <Button variant="secondary" size="sm" onClick={() => startEdit(c)}>수정</Button>
                    <Button variant="danger" size="sm" onClick={() => handleDelete(c.id)}>삭제</Button>
                  </>
                )}
              </div>
            </div>

            {editingId !== c.id ? (
              <>
                {c.role && <p className="item-field"><span>역할</span>{c.role}</p>}
                {c.age != null && <p className="item-field"><span>나이</span>{c.age}세</p>}
                {c.personality && <p className="item-field"><span>성격</span>{c.personality}</p>}
                {c.speechStyle && <p className="item-field"><span>말투</span>{c.speechStyle}</p>}
                {c.description && <p className="item-field"><span>설명</span>{c.description}</p>}
              </>
            ) : (
              <div className="inline-edit-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>이름 *</label>
                    <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} required />
                  </div>
                  <div className="form-group">
                    <label>역할</label>
                    <input type="text" value={editRole} onChange={(e) => setEditRole(e.target.value)} />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>나이</label>
                    <input type="number" value={editAge} onChange={(e) => setEditAge(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>말투</label>
                    <input type="text" value={editSpeechStyle} onChange={(e) => setEditSpeechStyle(e.target.value)} />
                  </div>
                </div>
                <div className="form-group">
                  <label>성격</label>
                  <input type="text" value={editPersonality} onChange={(e) => setEditPersonality(e.target.value)} />
                </div>
                <div className="form-group">
                  <label>설명</label>
                  <textarea value={editDescription} onChange={(e) => setEditDescription(e.target.value)} rows={3} />
                </div>
                <div className="form-actions">
                  <Button variant="primary" disabled={saving} onClick={() => handleUpdate(c.id)}>
                    {saving ? '저장 중...' : '저장'}
                  </Button>
                  <Button variant="secondary" onClick={() => setEditingId(null)}>취소</Button>
                </div>
              </div>
            )}
          </div>
        ))
      )}

      {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}
    </div>
  );
}
