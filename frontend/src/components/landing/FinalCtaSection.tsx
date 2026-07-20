import { useNavigate } from 'react-router-dom';
import Button from '../Button';

export default function FinalCtaSection() {
  const navigate = useNavigate();

  return (
    <section className="landing-final-cta">
      <h2>이야기에만 집중할 수 있는 집필 환경을 만들어보세요.</h2>
      <p>노벨네스트와 함께 등장인물, 세계관, 회차를 체계적으로 관리하세요.</p>
      <Button variant="primary" onClick={() => navigate('/login')}>
        무료로 시작하기
      </Button>
    </section>
  );
}