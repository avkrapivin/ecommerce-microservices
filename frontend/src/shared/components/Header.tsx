import React from 'react';
import { Layout, Button, Space } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { CartIcon } from './CartIcon';

const { Header: AntHeader } = Layout;

export function Header() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/auth/login');
  };

  return (
    <AntHeader style={{ 
      background: '#fff', 
      borderBottom: '1px solid #f0f0f0',
      padding: '0 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <Link to="/" style={{ 
          textDecoration: 'none', 
          color: '#1890ff',
          fontSize: '20px',
          fontWeight: 'bold',
          marginRight: '32px'
        }}>
          E-Commerce
        </Link>
        
        <Space size="large">
          <Link to="/catalog" style={{ color: '#666', textDecoration: 'none' }}>
            Catalog
          </Link>
        </Space>
      </div>
      
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {isAuthenticated ? (
          <>
            <CartIcon />
            <Link to="/profile">
              <Button type="link">Profile</Button>
            </Link>
            <Button type="text" onClick={handleLogout}>
              Logout
            </Button>
          </>
        ) : (
          <>
            <Link to="/auth/login">
              <Button type="link">Sign In</Button>
            </Link>
            <Link to="/auth/register">
              <Button type="primary">Register</Button>
            </Link>
          </>
        )}
      </div>
    </AntHeader>
  );
}
