import { useEffect, useState } from 'react';
import { Card, Col, Image, Rate, Row, Typography } from 'antd';
import { useParams } from 'react-router-dom';
import { getProductById, getProductImages, getProductReviews, ProductDto, ProductImageDto, ProductReviewDto } from '../shared/api/products';

const FALLBACK_IMG =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0nMzc2JyBoZWlnaHQ9JzIxMScgdmlld0JveD0nMCAwIDM3NiAyMTEnIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+PHJlY3Qgd2lkdGg9JzM3NicgaGVpZ2h0PScyMTEnIGZpbGw9JyNlZWUnLz48dGV4dCB4PScxOCUnIHk9JzUwJScgZmlsbD0nI2JiYicgZm9udC1zaXplPScxNnB4JyBmb250LWZhbWlseT0nQXJpYWwsIHNhbnMtc2VyaWYnPk5vIGltYWdlPC90ZXh0Pjwvc3ZnPg==';

export function Product() {
  const { id } = useParams();
  const productId = Number(id);

  const [product, setProduct] = useState<ProductDto | null>(null);
  const [images, setImages] = useState<ProductImageDto[]>([]);
  const [reviews, setReviews] = useState<ProductReviewDto[]>([]);

  useEffect(() => {
    if (!productId) return;
    getProductById(productId).then(setProduct).catch(() => setProduct(null));
    getProductImages(productId).then(setImages).catch(() => setImages([]));
    getProductReviews(productId)
      .then((data) => setReviews(Array.isArray(data) ? data : []))
      .catch(() => setReviews([]));
  }, [productId]);

  if (!product) return <Typography.Text>Product not found</Typography.Text>;

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card>
            <Row gutter={[8, 8]}>
              {images.length === 0 ? (
                <Col span={24}>
                  <Image src={FALLBACK_IMG} alt={product.name} preview={false} style={{ width: '100%' }} />
                </Col>
              ) : (
                images.map((img) => (
                  <Col span={12} key={img.id}>
                    <Image src={img.imageUrl} alt={product.name} fallback={FALLBACK_IMG} style={{ width: '100%' }} />
                  </Col>
                ))
              )}
            </Row>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card>
            <Typography.Title level={3}>{product.name}</Typography.Title>
            <Typography.Title level={4}>${product.price?.toFixed(2)}</Typography.Title>
            <Typography.Paragraph>{product.description || 'No description'}</Typography.Paragraph>
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <Typography.Title level={4}>Reviews</Typography.Title>
        {!Array.isArray(reviews) || reviews.length === 0 ? (
          <Typography.Text>No reviews yet</Typography.Text>
        ) : (
          reviews.map((r) => (
            <Card key={r.id} style={{ marginBottom: 8 }}>
              <Rate disabled value={r.rating} />
              <Typography.Paragraph style={{ marginTop: 8 }}>{r.comment}</Typography.Paragraph>
              <Typography.Text type="secondary">{r.userName}</Typography.Text>
            </Card>
          ))
        )}
      </Card>
    </div>
  );
}
