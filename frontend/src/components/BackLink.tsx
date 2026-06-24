interface BackLinkProps {
  label: string;
  onClick: () => void;
}

export default function BackLink({ label, onClick }: BackLinkProps) {
  return (
    <span className="back-link" onClick={onClick}>
      {label}
    </span>
  );
}
