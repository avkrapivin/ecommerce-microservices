import { Layout, Menu, Typography, Button, Space, message } from 'antd';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { getProfile, logout } from '../../shared/api/auth';

const { Header, Content, Footer } = Layout;

export function AppLayout() {
  const location = useLocation();
  const selectedKey = `/${location.pathname.split('/')[1] ?? ''}`;

  const [profileEmail, setProfileEmail] = useState<string | null>(null);

  useEffect(() => {
    // Try to load profile to determine auth state
    getProfile()
      .then((p) => setProfileEmail(p.email))
      .catch(() => setProfileEmail(null));
  }, [location.pathname]);

  const onLogout = async () => {
    try {
      await logout();
      message.success('Signed out');
      window.location.href = '/';
    } catch {
      message.error('Failed to sign out');
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0, marginRight: 24 }}>
          E-Commerce
        </Typography.Title>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={[
            { key: '/', label: <Link to="/">Home</Link> },
            { key: '/catalog', label: <Link to="/catalog">Catalog</Link> },
            { key: '/profile', label: <Link to="/profile">Profile</Link> },
          ]}
          style={{ flex: 1 }}
        />
        <Space>
          {profileEmail ? (
            <>
              <Typography.Text style={{ color: '#fff' }}>{profileEmail}</Typography.Text>
              <Button onClick={onLogout}>Logout</Button>
            </>
          ) : (
            <Link to="/auth/login">
              <Button type="primary">Sign in</Button>
            </Link>
          )}
        </Space>
      </Header>
      <Content style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>E-Commerce © {new Date().getFullYear()}</Footer>
    </Layout>
  );
}
