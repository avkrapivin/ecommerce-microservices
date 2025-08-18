import { useEffect, useState } from 'react';
import { Card, Col, Image, Rate, Row, Skeleton, Typography, Empty, Button, Space, Divider } from 'antd';
import { useParams } from 'react-router-dom';
import { getProduct } from '../shared/api/products';
import type { Product } from '../shared/api/products';
import { AddToCartButton } from '../shared/components/AddToCartButton';

const FALLBACK_IMG =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0nMzc2JyBoZWlnaHQ9JzIxMScgdmlld0JveD0nMCAwIDM3NiAyMTEnIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+PHJlY3Qgd2lkdGg9JzM3NicgaGVpZ2h0PScyMTEnIGZpbGw9JyNlZWUnLz48dGV4dCB4PScxOCUnIHk9JzUwJScgZmlsbD0nI2JiYicgZm9udC1zaXplPScxNnB4JyBmb250LWZhbWlseT0nQXJpYWwsIHNhbnMtc2VyaWYnPk5vIGltYWdlPC90ZXh0Pjwvc3ZnPg==';

export function Product() {
  const { id } = useParams();
  const productId = Number(id);

  const [loading, setLoading] = useState(true);
  const [product, setProduct] = useState<Product | null>(null);

  useEffect(() => {
    if (!productId) return;
    (async () => {
      setLoading(true);
      try {
        const productData = await getProduct(productId);
        setProduct(productData);
      } catch (error) {
        console.error('Failed to load product:', error);
      } finally {
        setLoading(false);
      }
    })();
  }, [productId]);

  if (loading) {
    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card>
            <Skeleton.Image style={{ width: '100%', height: 240 }} active />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card>
            <Skeleton active paragraph={{ rows: 6 }} title />
          </Card>
        </Col>
        <Col span={24}>
          <Card>
            <Skeleton active paragraph={{ rows: 3 }} title={{ width: '30%' }} />
          </Card>
        </Col>
      </Row>
    );
  }

  if (!product) return <Empty description="Product not found" />;

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card>
            <Image 
              src={product.images?.find((i) => i.isMain)?.imageUrl || product.images?.[0]?.imageUrl || FALLBACK_IMG} 
              alt={product.name} 
              fallback={FALLBACK_IMG} 
              style={{ width: '100%' }} 
            />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card>
            <Typography.Title level={3}>{product.name}</Typography.Title>
            <Typography.Title level={4}>${product.price.toFixed(2)}</Typography.Title>
            <Typography.Paragraph>{product.description || 'No description'}</Typography.Paragraph>
            
            <Divider />
            
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              <div>
                <Typography.Text strong>Stock: </Typography.Text>
                <Typography.Text>{product.stockQuantity}</Typography.Text>
              </div>
              
              {product.reviews && product.reviews.length > 0 ? (
                <div>
                  <Typography.Text strong>Rating: </Typography.Text>
                  <Rate disabled value={product.reviews[0].rating} />
                  <Typography.Text> ({product.reviews.length} reviews)</Typography.Text>
                </div>
              ) : (
                <div>
                  <Typography.Text strong>Rating: </Typography.Text>
                  <Typography.Text type="secondary">No reviews yet</Typography.Text>
                </div>
              )}
              
              <AddToCartButton 
                product={{
                  id: product.id,
                  name: product.name,
                  price: product.price,
                  image: product.images?.find((i) => i.isMain)?.imageUrl || product.images?.[0]?.imageUrl || '',
                }}
                disabled={product.stockQuantity === 0}
              />
            </Space>
          </Card>
        </Col>
      </Row>

      {/* Reviews Section */}
      <Divider orientation="left" style={{ marginTop: 32 }}>
        Reviews ({product.reviews?.length || 0})
      </Divider>
      
      {product.reviews && product.reviews.length > 0 ? (
        <Row gutter={[16, 16]}>
          {product.reviews.map((review) => (
            <Col span={24} key={review.id}>
              <Card size="small">
                <Space direction="vertical" size="small" style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Space>
                      <Rate disabled value={review.rating} />
                      <Typography.Text strong>User {review.userId}</Typography.Text>
                    </Space>
                    <Typography.Text type="secondary" style={{ fontSize: '12px' }}>
                      {new Date(review.createdAt).toLocaleDateString()}
                    </Typography.Text>
                  </div>
                  <Typography.Paragraph style={{ margin: 0 }}>
                    {review.comment}
                  </Typography.Paragraph>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      ) : (
        <Card>
          <Typography.Text type="secondary">No reviews yet. Be the first to review this product!</Typography.Text>
        </Card>
      )}
    </div>
  );
}
