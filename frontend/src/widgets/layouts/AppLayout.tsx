import { Layout } from 'antd';
import { Outlet } from 'react-router-dom';
import { Header } from '../../shared/components/Header';

const { Content, Footer } = Layout;

export function AppLayout() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header />
      <Content style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>E-Commerce © {new Date().getFullYear()}</Footer>
    </Layout>
  );
}
