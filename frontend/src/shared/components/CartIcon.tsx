import React from 'react';
import { Badge, Button } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useCart } from '../contexts/CartContext';
import { useNavigate } from 'react-router-dom';

export function CartIcon() {
  const { state } = useCart();
  const navigate = useNavigate();

  const handleClick = () => {
    navigate('/cart');
  };

  return (
    <Badge count={state.itemCount} size="small">
      <Button
        type="text"
        icon={<ShoppingCartOutlined />}
        onClick={handleClick}
        style={{ color: 'inherit' }}
      />
    </Badge>
  );
}
