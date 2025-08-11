import { Layout, Menu, Typography } from 'antd';
import { Link, Outlet, useLocation } from 'react-router-dom';

const { Header, Content, Footer } = Layout;

export function AppLayout() {
  const location = useLocation();
  const selectedKey = `/${location.pathname.split('/')[1] ?? ''}`;

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
      </Header>
      <Content style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>E-Commerce © {new Date().getFullYear()}</Footer>
    </Layout>
  );
}
