import { useTheme } from '../hooks/useTheme';

const LABEL = {
  light: '라이트 모드',
  dark: '다크 모드',
} as const;

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const icon = theme === 'dark' ? '🌙' : '☀️';

  return (
    <button
      type="button"
      className="theme-toggle-btn"
      onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
      aria-label={`현재 테마: ${LABEL[theme]}. 클릭하면 ${theme === 'dark' ? '라이트' : '다크'} 모드로 전환됩니다.`}
      title={LABEL[theme]}
    >
      <span aria-hidden="true">{icon}</span>
    </button>
  );
}
