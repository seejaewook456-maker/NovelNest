import { useState, useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getNovel } from '../api/novelApi';
import { getContextStats, sendChatMessage } from '../api/chatApi';
import type { Novel } from '../types/novel';
import type { ContextStats, ChatMessage } from '../types/chat';
import BackLink from '../components/BackLink';
import LoadingSpinner from '../components/LoadingSpinner';

// 간단한 인라인 마크다운 파싱 — bold, italic, code 처리
function parseInline(text: string): ReactNode[] {
  const parts: ReactNode[] = [];
  const regex = /(\*\*(.+?)\*\*)|(\*(.+?)\*)|(`(.+?)`)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    if (match[1]) {
      parts.push(<strong key={match.index}>{match[2]}</strong>);
    } else if (match[3]) {
      parts.push(<em key={match.index}>{match[4]}</em>);
    } else if (match[5]) {
      parts.push(<code key={match.index} className="chat-inline-code">{match[6]}</code>);
    }
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }
  return parts;
}

// 마크다운 블록 렌더러 — 단락, 제목, 목록 처리
function MarkdownRenderer({ content }: { content: string }) {
  const blocks = content.split(/\n\n+/);

  return (
    <div className="chat-markdown">
      {blocks.map((block, blockIdx) => {
        const lines = block.split('\n').filter(l => l.trim() !== '');

        // 제목 (#, ##, ###)
        if (lines.length === 1) {
          if (lines[0].startsWith('### ')) {
            return <h5 key={blockIdx} className="chat-md-h5">{parseInline(lines[0].slice(4))}</h5>;
          }
          if (lines[0].startsWith('## ')) {
            return <h4 key={blockIdx} className="chat-md-h4">{parseInline(lines[0].slice(3))}</h4>;
          }
          if (lines[0].startsWith('# ')) {
            return <h3 key={blockIdx} className="chat-md-h3">{parseInline(lines[0].slice(2))}</h3>;
          }
        }

        // 목록 (- 또는 숫자.)
        const isUnordered = lines.every(l => /^[-*]\s/.test(l));
        const isOrdered = lines.every(l => /^\d+\.\s/.test(l));

        if (isUnordered) {
          return (
            <ul key={blockIdx} className="chat-md-list">
              {lines.map((l, i) => (
                <li key={i}>{parseInline(l.replace(/^[-*]\s/, ''))}</li>
              ))}
            </ul>
          );
        }

        if (isOrdered) {
          return (
            <ol key={blockIdx} className="chat-md-list">
              {lines.map((l, i) => (
                <li key={i}>{parseInline(l.replace(/^\d+\.\s/, ''))}</li>
              ))}
            </ol>
          );
        }

        // 일반 단락 — 줄바꿈(\n) 보존
        return (
          <p key={blockIdx} className="chat-md-p">
            {lines.map((l, i) => (
              <span key={i}>
                {parseInline(l)}
                {i < lines.length - 1 && <br />}
              </span>
            ))}
          </p>
        );
      })}
    </div>
  );
}

export default function NovelDetailPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();

  const [novel, setNovel] = useState<Novel | null>(null);
  const [error, setError] = useState('');

  // 챗봇 상태
  const [contextStats, setContextStats] = useState<ContextStats | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [chatError, setChatError] = useState('');
  // 복사 피드백 — 메시지 id로 어떤 버튼이 활성화됐는지 추적
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [copyFailedId, setCopyFailedId] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!novelId) return;
    const id = Number(novelId);
    getNovel(id)
      .then(setNovel)
      .catch((err) => setError(err instanceof Error ? err.message : '조회 실패'));
    getContextStats(id)
      .then(setContextStats)
      .catch(() => {/* 통계 실패는 챗봇 자체를 막지 않음 */});
  }, [novelId]);

  // 새 메시지가 추가될 때마다 맨 아래로 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    const trimmed = inputMessage.trim();
    if (!trimmed || isChatLoading) return;

    const userMsg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: trimmed,
    };

    setMessages(prev => [...prev, userMsg]);
    setInputMessage('');
    setIsChatLoading(true);
    setChatError('');

    try {
      const answer = await sendChatMessage(Number(novelId), trimmed);
      const assistantMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: answer,
      };
      setMessages(prev => [...prev, assistantMsg]);
    } catch (err) {
      setChatError(err instanceof Error ? err.message : 'AI 답변 생성에 실패했습니다.');
    } finally {
      setIsChatLoading(false);
    }
  };

  const handleCopy = async (id: string, content: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      setCopyFailedId(id);
      setTimeout(() => setCopyFailedId(null), 2000);
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Enter 전송, Shift+Enter 줄바꿈
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (error) return <p className="error-message">{error}</p>;
  if (!novel) return <LoadingSpinner />;

  const id = Number(novelId);
  const summaryRatio = contextStats
    ? Math.round((contextStats.summaryCount / Math.max(contextStats.totalEpisodeCount, 1)) * 100)
    : 0;
  const hasIncompleteSummaries =
    contextStats !== null &&
    contextStats.totalEpisodeCount > 0 &&
    contextStats.summaryCount < contextStats.totalEpisodeCount;

  return (
    <div>
      <BackLink label="← 작품 목록" onClick={() => navigate('/novels')} />

      <div className="novel-info-card">
        <h2>{novel.title}</h2>
        <span className="genre-badge">{novel.genre}</span>
        {novel.description && <p className="description">{novel.description}</p>}
      </div>

      <div className="section-cards">
        <div className="section-card" onClick={() => navigate(`/novels/${id}/episodes`)}>
          <h3>회차 관리</h3>
          <p>회차 목록 조회 및 작성</p>
        </div>
        <div className="section-card" onClick={() => navigate(`/novels/${id}/characters`)}>
          <h3>등장인물 관리</h3>
          <p>인물 추가 및 수정</p>
        </div>
        <div className="section-card" onClick={() => navigate(`/novels/${id}/world-settings`)}>
          <h3>세계관 관리</h3>
          <p>설정 추가 및 수정</p>
        </div>
      </div>

      {/* AI Writing Assistant */}
      <div className="ai-section chat-section">
        <div className="ai-section-header">
          <h3>AI Writing Assistant</h3>
        </div>

        {/* AI 데이터 현황 */}
        {contextStats && (
          <div className="chat-stats-bar">
            <div className="chat-stats-items">
              <span className="chat-stat-item">
                <span className="chat-stat-label">회차 요약</span>
                <span className="chat-stat-value">
                  {contextStats.summaryCount}/{contextStats.totalEpisodeCount}
                </span>
              </span>
              <span className="chat-stat-divider">|</span>
              <span className="chat-stat-item">
                <span className="chat-stat-label">등장인물</span>
                <span className="chat-stat-value">{contextStats.characterCount}명</span>
              </span>
              <span className="chat-stat-divider">|</span>
              <span className="chat-stat-item">
                <span className="chat-stat-label">세계관 설정</span>
                <span className="chat-stat-value">{contextStats.worldSettingCount}개</span>
              </span>
            </div>
            {contextStats.totalEpisodeCount > 0 && (
              <div className="chat-stats-progress">
                <div className="progress-bar-track">
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${summaryRatio}%` }}
                  />
                </div>
                <span className="progress-bar-label">{summaryRatio}%</span>
              </div>
            )}
          </div>
        )}

        {/* 요약 미완성 경고 */}
        {hasIncompleteSummaries && (
          <div className="chat-warning">
            <span className="chat-warning-text">
              요약이 없는 회차 내용은 AI가 참고할 수 없습니다.
              더 정확한 답변을 위해 회차 요약을 추가해 보세요.
            </span>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => navigate(`/novels/${id}/episodes`)}
            >
              회차 관리로 이동
            </button>
          </div>
        )}

        {/* 대화 영역 */}
        <div className="chat-messages">
          {messages.length === 0 && (
            <div className="chat-empty">
              <p className="chat-empty-text">
                작품의 등장인물, 세계관, 회차 요약을 바탕으로 질문에 답합니다.
              </p>
              <div className="chat-suggestions">
                <button className="chat-suggestion-chip" onClick={() => setInputMessage('지금까지의 스토리를 요약해줘')}>
                  지금까지의 스토리를 요약해줘
                </button>
                <button className="chat-suggestion-chip" onClick={() => setInputMessage('주인공의 성격과 말투를 설명해줘')}>
                  주인공의 성격과 말투를 설명해줘
                </button>
                <button className="chat-suggestion-chip" onClick={() => setInputMessage('이 작품의 세계관 핵심 규칙을 알려줘')}>
                  이 작품의 세계관 핵심 규칙을 알려줘
                </button>
              </div>
            </div>
          )}

          {messages.map(msg => (
            <div key={msg.id} className={`chat-message chat-message-${msg.role}`}>
              <div className="chat-bubble">
                {msg.role === 'assistant' ? (
                  <>
                    <MarkdownRenderer content={msg.content} />
                    <div className="chat-copy-area">
                      <button
                        className="btn btn-ghost btn-sm chat-copy-btn"
                        onClick={() => handleCopy(msg.id, msg.content)}
                      >
                        {copiedId === msg.id
                          ? '복사되었습니다.'
                          : copyFailedId === msg.id
                          ? '복사에 실패했습니다.'
                          : '답변 복사'}
                      </button>
                    </div>
                  </>
                ) : (
                  <p className="chat-user-text">{msg.content}</p>
                )}
              </div>
            </div>
          ))}

          {isChatLoading && (
            <div className="chat-message chat-message-assistant">
              <div className="chat-bubble chat-loading-bubble">
                <span className="chat-loading-dot" />
                <span className="chat-loading-dot" />
                <span className="chat-loading-dot" />
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {chatError && <p className="error-message">{chatError}</p>}

        {/* 입력 영역 */}
        <div className="chat-input-area">
          <textarea
            className="chat-textarea"
            value={inputMessage}
            onChange={e => setInputMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="작품에 대해 질문해 보세요... (Enter로 전송, Shift+Enter로 줄바꿈)"
            rows={3}
            disabled={isChatLoading}
          />
          <button
            className="btn btn-ai chat-send-btn"
            onClick={handleSend}
            disabled={isChatLoading || !inputMessage.trim()}
          >
            {isChatLoading ? '생성 중...' : '전송'}
          </button>
        </div>
      </div>
    </div>
  );
}
