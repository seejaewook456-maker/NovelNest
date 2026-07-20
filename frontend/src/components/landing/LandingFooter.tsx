// 이용약관 / 개인정보처리방침 페이지가 프로젝트에 아직 없어, 클릭해도 아무 동작을 하지 않는
// 가짜 링크를 두는 대신 해당 페이지가 추가될 때까지 항목 자체를 노출하지 않는다.
export default function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="landing-footer-inner">
        <div className="landing-footer-brand">
          <span className="landing-footer-logo">노벨네스트</span>
          <p>AI 기반 웹소설 집필 도구</p>
        </div>
        <p className="landing-footer-copyright">
          &copy; {new Date().getFullYear()} 노벨네스트. All rights reserved.
        </p>
      </div>
    </footer>
  );
}