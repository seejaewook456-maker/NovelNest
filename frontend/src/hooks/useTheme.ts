import { useSyncExternalStore } from 'react';
import { getTheme, setTheme, subscribeTheme } from '../state/themeStore';
import type { Theme } from '../state/themeStore';

interface UseThemeResult {
  theme: Theme;
  setTheme: (next: Theme) => void;
}

// aiUsageStore를 구독하는 useAiDailyUsage.ts와 동일한 방식.
export function useTheme(): UseThemeResult {
  const theme = useSyncExternalStore(subscribeTheme, getTheme);

  return { theme, setTheme };
}
