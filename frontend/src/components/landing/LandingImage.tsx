import { useState, type ImgHTMLAttributes } from 'react';
import { useTheme } from '../../hooks/useTheme';

interface LandingImageProps extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'loading' | 'src'> {
  // 라이트/다크 테마별 캡처 이미지 경로. 현재 테마에 맞는 한 장만 렌더링(=다운로드)한다.
  lightSrc: string;
  darkSrc: string;
  // 이미지 영역(박스)의 기준 크기. 라이트/다크 캡처의 픽셀 크기가 조금씩 달라도
  // 이 비율로 박스를 고정해 테마 전환 시 레이아웃이 흔들리지 않게 한다.
  width: number;
  height: number;
  // Hero 이미지처럼 첫 화면 노출 속도가 중요한 경우 true로 넘겨 lazy loading을 끈다.
  eager?: boolean;
}

// 랜딩 페이지 캡처 이미지 전용 래퍼.
// - 테마 상태는 별도로 만들지 않고 기존 themeStore(useTheme)를 그대로 구독한다.
//   themeStore는 모듈 로드 시 localStorage를 읽어 두므로 첫 렌더부터 올바른 테마 이미지가 선택된다.
// - 이미지 파일이 없거나 로드에 실패해도 깨진 이미지 아이콘 대신 안내 문구를 보여줘
//   페이지 레이아웃이 무너지지 않도록 한다.
export default function LandingImage({
  lightSrc,
  darkSrc,
  width,
  height,
  eager = false,
  alt,
  className,
  style,
  ...rest
}: LandingImageProps) {
  const { theme } = useTheme();
  const src = theme === 'dark' ? darkSrc : lightSrc;

  // 실패한 "경로"를 기억한다. 테마가 바뀌어 다른 경로로 전환되면 자동으로 다시 시도하게 된다.
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const boxStyle = { aspectRatio: `${width} / ${height}`, ...style };

  if (failedSrc === src) {
    return (
      <div
        className={`landing-image-fallback ${className ?? ''}`.trim()}
        style={boxStyle}
        role="img"
        aria-label={alt}
      >
        이미지를 준비 중입니다
      </div>
    );
  }

  return (
    <img
      {...rest}
      src={src}
      alt={alt}
      width={width}
      height={height}
      className={className}
      style={boxStyle}
      loading={eager ? 'eager' : 'lazy'}
      decoding="async"
      onError={() => setFailedSrc(src)}
    />
  );
}
