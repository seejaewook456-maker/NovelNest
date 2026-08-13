import { useTheme } from '../hooks/useTheme';

const LABEL = {
  light: '라이트 모드',
  dark: '다크 모드',
} as const;

// 회차 작성 메뉴(EpisodeToolRail)와 동일한 인라인 SVG 라인 아이콘 컨벤션 —
// 이모지 대신 currentColor를 상속받는 심플한 선 아이콘으로 OS/브라우저 무관하게 통일된 모습을 낸다.
const SunIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
  </svg>
);

const MoonIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
  </svg>
);

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <button
      type="button"
      className="theme-toggle-btn"
      onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
      aria-label={`현재 테마: ${LABEL[theme]}. 클릭하면 ${theme === 'dark' ? '라이트' : '다크'} 모드로 전환됩니다.`}
      title={LABEL[theme]}
    >
      {theme === 'dark' ? <MoonIcon /> : <SunIcon />}
    </button>
  );
}
