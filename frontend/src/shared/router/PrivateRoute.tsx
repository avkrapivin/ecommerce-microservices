import { PropsWithChildren, useEffect, useState } from 'react';
import { getProfile } from '../../shared/api/auth';
import { Card, Skeleton } from 'antd';

export function PrivateRoute({ children }: PropsWithChildren) {
  const [checking, setChecking] = useState(true);
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setChecking(true);
      try {
        await getProfile();
        if (!cancelled) setAllowed(true);
      } catch {
        const next = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/auth/login?next=${next}`;
      } finally {
        if (!cancelled) setChecking(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (checking) {
    return (
      <Card>
        <Skeleton active paragraph={{ rows: 4 }} title />
      </Card>
    );
  }

  if (!allowed) return null;
  return <>{children}</>;
}


