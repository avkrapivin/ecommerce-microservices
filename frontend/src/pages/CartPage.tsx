import React from 'react';
import { 
  Card, 
  Button, 
  InputNumber, 
  Space, 
  Typography, 
  Empty, 
  Divider,
  message,
  Popconfirm
} from 'antd';
import { DeleteOutlined, ShoppingOutlined } from '@ant-design/icons';
import { useCart } from '../shared/contexts/CartContext';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;

export function CartPage() {
  const { state, removeItem, updateQuantity, clearCart } = useCart();
  const navigate = useNavigate();

  const handleQuantityChange = (id: number, quantity: number) => {
    if (quantity < 1) {
      message.warning('Quantity must be greater than 0');
      return;
    }
    updateQuantity(id, quantity);
  };

  const handleRemoveItem = (id: number) => {
    removeItem(id);
    message.success('Product removed from cart');
  };

  const handleClearCart = () => {
    clearCart();
    message.success('Cart cleared');
  };

  const handleCheckout = () => {
    // TODO: Implement checkout logic with reservations
    message.info('Proceeding to checkout...');
    navigate('/checkout');
  };

  if (state.items.length === 0) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Empty
          image={<ShoppingOutlined style={{ fontSize: '64px', color: '#d9d9d9' }} />}
          description="Your cart is empty"
        >
          <Button type="primary" onClick={() => navigate('/catalog')}>
            Go to Catalog
          </Button>
        </Empty>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <Title level={2}>Cart</Title>
        <Space>
          <Popconfirm
            title="Clear cart?"
            description="All products will be removed from cart"
            onConfirm={handleClearCart}
            okText="Yes"
            cancelText="No"
          >
            <Button danger icon={<DeleteOutlined />}>
              Clear Cart
            </Button>
          </Popconfirm>
        </Space>
      </div>

      <div style={{ display: 'flex', gap: '24px' }}>
        {/* Cart Items */}
        <div style={{ flex: 1 }}>
          {state.items.map((item) => (
            <Card 
              key={item.id} 
              style={{ marginBottom: '16px' }}
              bodyStyle={{ padding: '16px' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <img 
                  src={item.image} 
                  alt={item.name}
                  style={{ 
                    width: '80px', 
                    height: '80px', 
                    objectFit: 'cover',
                    borderRadius: '8px'
                  }}
                />
                
                <div style={{ flex: 1 }}>
                  <Title level={5} style={{ margin: 0, marginBottom: '8px' }}>
                    {item.name}
                  </Title>
                  <Text strong style={{ fontSize: '16px', color: '#1890ff' }}>
                    ${item.price.toFixed(2)}
                  </Text>
                </div>
                
                <Space direction="vertical" align="center">
                  <Text>Quantity:</Text>
                  <InputNumber
                    min={1}
                    max={99}
                    value={item.quantity}
                    onChange={(value) => handleQuantityChange(item.id, value || 1)}
                    style={{ width: '80px' }}
                  />
                </Space>
                
                <div style={{ textAlign: 'center', minWidth: '100px' }}>
                  <Text strong style={{ fontSize: '16px' }}>
                    ${(item.price * item.quantity).toFixed(2)}
                  </Text>
                </div>
                
                <Popconfirm
                  title="Remove product?"
                  description="Product will be removed from cart"
                  onConfirm={() => handleRemoveItem(item.id)}
                  okText="Yes"
                  cancelText="No"
                >
                  <Button 
                    type="text" 
                    danger 
                    icon={<DeleteOutlined />}
                    size="small"
                  />
                </Popconfirm>
              </div>
            </Card>
          ))}
        </div>

        {/* Order Summary */}
        <Card style={{ width: '300px', height: 'fit-content' }}>
          <Title level={4}>Total</Title>
          <Divider />
          
          <div style={{ marginBottom: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <Text>Items:</Text>
              <Text>{state.itemCount}</Text>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <Text>Products:</Text>
              <Text>{state.items.length}</Text>
            </div>
          </div>
          
          <Divider />
          
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px' }}>
            <Text strong style={{ fontSize: '18px' }}>Total:</Text>
            <Text strong style={{ fontSize: '20px', color: '#1890ff' }}>
              ${state.total.toFixed(2)}
            </Text>
          </div>
          
          <Button 
            type="primary" 
            size="large" 
            block
            onClick={handleCheckout}
            disabled={state.items.length === 0}
          >
            Checkout
          </Button>
        </Card>
      </div>
    </div>
  );
}
