import { useEffect, useMemo, useState } from 'react';
import { Card, Col, Divider, Empty, Image, Row, Skeleton, Typography } from 'antd';
import { getCategories, getProducts, Product } from '../shared/api/products';
import { useNavigate } from 'react-router-dom';

// Fallback image for broken URLs
const FALLBACK_IMG =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0nMzc2JyBoZWlnaHQ9JzIxMScgdmlld0JveD0nMCAwIDM3NiAyMTEnIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+PHJlY3Qgd2lkdGg9JzM3NicgaGVpZ2h0PScyMTEnIGZpbGw9JyNlZWUnLz48dGV4dCB4PScxOCUnIHk9JzUwJScgZmlsbD0nI2JiYicgZm9udC1zaXplPScxNnB4JyBmb250LWZhbWlseT0nQXJpYWwsIHNhbnMtc2VyaWYnPk5vIGltYWdlPC90ZXh0Pjwvc3ZnPg==';

export function Home() {
  const navigate = useNavigate();

  // Server data state
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [newArrivals, setNewArrivals] = useState<Product[]>([]);
  const [categories, setCategories] = useState<{ id: number; name: string; description?: string }[]>(
    [],
  );

  useEffect(() => {
    // Load new arrivals (sorted by createdAt desc)
    (async () => {
      setLoadingProducts(true);
      try {
        const data = await getProducts(0, 12, undefined, undefined, undefined, undefined, 'createdAt', 'desc');
        setNewArrivals(data.content);
      } finally {
        setLoadingProducts(false);
      }
    })();

    // Load categories (we will show root/top-level categories)
    (async () => {
      setLoadingCategories(true);
      try {
        const data = await getCategories();
        setCategories(data);
      } finally {
        setLoadingCategories(false);
      }
    })();
  }, []);

          const rootCategories = useMemo(
          () => categories.slice(0, 8),
          [categories],
        );

  return (
    <div>
      <Typography.Title level={3}>Welcome to our store</Typography.Title>
      <Typography.Paragraph>
        Discover the latest arrivals and explore top categories.
      </Typography.Paragraph>

      <Divider orientation="left">Top categories</Divider>
      {loadingCategories ? (
        <Row gutter={[16, 16]}>
          {Array.from({ length: 8 }).map((_, i) => (
            <Col key={i} xs={12} sm={8} md={6} lg={6}>
              <Card>
                <Skeleton active paragraph={{ rows: 1 }} title={{ width: '60%' }} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : rootCategories.length === 0 ? (
        <Empty description="No categories" />
      ) : (
        <Row gutter={[16, 16]}>
          {rootCategories.map((cat) => (
            <Col key={cat.id} xs={12} sm={8} md={6} lg={6}>
              <Card
                hoverable
                onClick={() => navigate(`/catalog?categoryId=${cat.id}`)}
                title={cat.name}
              >
                <Typography.Text type="secondary">Explore {cat.name}</Typography.Text>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Divider orientation="left" style={{ marginTop: 24 }}>
        New arrivals
      </Divider>
      {loadingProducts ? (
        <Row gutter={[16, 16]}>
          {Array.from({ length: 12 }).map((_, i) => (
            <Col key={i} xs={24} sm={12} md={8} lg={6}>
              <Card>
                <Skeleton.Image style={{ width: '100%', height: 160 }} active />
                <Skeleton active paragraph={{ rows: 1 }} title />
              </Card>
            </Col>
          ))}
        </Row>
      ) : newArrivals.length === 0 ? (
        <Empty description="No products yet" />
      ) : (
        <Row gutter={[16, 16]}>
          {newArrivals.map((p) => {
            const preview = p.images?.find((i) => i.isMain)?.imageUrl || p.images?.[0]?.imageUrl;
            return (
              <Col key={p.id} xs={24} sm={12} md={8} lg={6}>
                <Card
                  hoverable
                  cover={
                    <Image
                      src={preview}
                      alt={p.name}
                      fallback={FALLBACK_IMG}
                      preview={false}
                      height={160}
                      style={{ objectFit: 'cover' }}
                    />
                  }
                  onClick={() => navigate(`/product/${p.id}`)}
                >
                  <Typography.Text strong>{p.name}</Typography.Text>
                  <div style={{ marginTop: 8 }}>${p.price.toFixed(2)}</div>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </div>
  );
}
