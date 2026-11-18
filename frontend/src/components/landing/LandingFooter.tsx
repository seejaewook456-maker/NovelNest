// 이용약관 / 개인정보처리방침 페이지가 프로젝트에 아직 없어, 클릭해도 아무 동작을 하지 않는
// 가짜 링크를 두는 대신 해당 페이지가 추가될 때까지 항목 자체를 노출하지 않는다.

// 서비스 문의 이메일 — 푸터에 노출해 사용자·외부 심사(AWS Activate 등)가 연락처를 확인할 수 있게 한다.
const CONTACT_EMAIL = 'contact@novelnestia.com';

export default function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="landing-footer-inner">
        <div className="landing-footer-brand">
          <span className="landing-footer-logo">NovelNest</span>
          <p>AI 기반 웹소설 집필 도구</p>
        </div>
        <div className="landing-footer-meta">
          <p className="landing-footer-contact">
            문의: <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
          </p>
          {/* 연도는 현재 연도로 자동 표시되므로(2026년 기준 "© 2026 NovelNest") 매년 수정할 필요가 없다. */}
          <p className="landing-footer-copyright">
            &copy; {new Date().getFullYear()} NovelNest. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
