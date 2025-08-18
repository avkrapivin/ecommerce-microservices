import React, { useState } from 'react';
import { Button, InputNumber, Space, message } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useCart } from '../contexts/CartContext';
import { CartItem } from '../contexts/CartContext';

interface AddToCartButtonProps {
  product: {
    id: number;
    name: string;
    price: number;
    image: string;
  };
  disabled?: boolean;
}

export function AddToCartButton({ product, disabled = false }: AddToCartButtonProps) {
  const { addItem } = useCart();
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);

  const handleAddToCart = async () => {
    if (quantity < 1) {
      message.warning('Quantity must be greater than 0');
      return;
    }

    setLoading(true);
    
    try {
      addItem({
        ...product,
        quantity,
      });
      
      message.success(`Product "${product.name}" added to cart`);
      setQuantity(1); // Reset quantity after adding
    } catch (error) {
      message.error('Failed to add product to cart');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Space>
      <InputNumber
        min={1}
        max={99}
        value={quantity}
        onChange={(value) => setQuantity(value || 1)}
        style={{ width: 80 }}
      />
      <Button
        type="primary"
        icon={<ShoppingCartOutlined />}
        onClick={handleAddToCart}
        loading={loading}
        disabled={disabled}
      >
        Add to Cart
      </Button>
    </Space>
  );
}
