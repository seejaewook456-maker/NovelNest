import { useState, type ImgHTMLAttributes } from 'react';

interface LandingImageProps extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'loading'> {
  // Hero 이미지처럼 첫 화면 노출 속도가 중요한 경우 true로 넘겨 lazy loading을 끈다.
  eager?: boolean;
}

// 랜딩 페이지 캡처 이미지 전용 래퍼.
// 이미지 파일이 아직 없거나 로드에 실패해도 깨진 이미지 아이콘 대신 안내 문구를 보여줘
// 페이지 레이아웃이 무너지지 않도록 한다.
export default function LandingImage({ eager = false, alt, className, ...rest }: LandingImageProps) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <div className={`landing-image-fallback ${className ?? ''}`.trim()} role="img" aria-label={alt}>
        이미지를 준비 중입니다
      </div>
    );
  }

  return (
    <img
      {...rest}
      alt={alt}
      className={className}
      loading={eager ? 'eager' : 'lazy'}
      onError={() => setFailed(true)}
    />
  );
}