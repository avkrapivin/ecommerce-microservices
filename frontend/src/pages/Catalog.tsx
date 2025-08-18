import { useEffect, useMemo, useState } from 'react';
import { Card, Input, Select, Row, Col, Pagination, Typography, Space, Divider, Spin, Image, Skeleton, Empty, Slider } from 'antd';
import { getCategories, getProducts, Product } from '../shared/api/products';
import { useNavigate, useSearchParams } from 'react-router-dom';

const PAGE_SIZE_OPTIONS = [12, 24, 48];
const FALLBACK_IMG =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0nMzc2JyBoZWlnaHQ9JzIxMScgdmlld0JveD0nMCAwIDM3NiAyMTEnIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+PHJlY3Qgd2lkdGg9JzM3NicgaGVpZ2h0PScyMTEnIGZpbGw9JyNlZWUnLz48dGV4dCB4PScxOCUnIHk9JzUwJScgZmlsbD0nI2JiYicgZm9udC1zaXplPScxNnB4JyBmb250LWZhbWlseT0nQXJpYWwsIHNhbnMtc2VyaWYnPk5vIGltYWdlPC90ZXh0Pjwvc3ZnPg==';

export function Catalog() {
  const [loading, setLoading] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filters state
  const [search, setSearch] = useState<string>('');
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);
  const [size, setSize] = useState<number>(24);
  const [page, setPage] = useState<number>(1); // 1-based for UI
  const [sortBy, setSortBy] = useState<string | undefined>(undefined);
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc' | undefined>(undefined);
  const [priceRange, setPriceRange] = useState<[number, number] | null>(null);

  const [categories, setCategories] = useState<{ label: string; value: number }[]>([]);

  // Initialize categoryId from query string
  useEffect(() => {
    const catId = searchParams.get('categoryId');
    if (catId) {
      const parsed = Number(catId);
      if (!Number.isNaN(parsed)) setCategoryId(parsed);
    }
  }, []);

  useEffect(() => {
    // Load categories once
    getCategories()
      .then((list) => setCategories(list.map((c) => ({ label: c.name, value: c.id }))))
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      setLoading(true);
      try {
        const data = await getProducts(
          page - 1, 
          size,
          search || undefined,
          categoryId,
          priceRange ? priceRange[0] : undefined,
          priceRange ? priceRange[1] : undefined,
          sortBy,
          sortDirection
        );
        setProducts(data.content);
        setTotal(data.totalElements);
      } catch (e: any) {
        if (e?.name !== 'CanceledError' && e?.message !== 'canceled') {
          console.error(e);
        }
      } finally {
        setLoading(false);
      }
    };
    load();
    return () => controller.abort();
  }, [search, categoryId, page, size, sortBy, sortDirection, priceRange]);

  const onCategoryChange = (v: number | undefined) => {
    setPage(1);
    setCategoryId(v);
    if (v) setSearchParams({ categoryId: String(v) });
    else setSearchParams({});
  };

  return (
    <div>
      <Typography.Title level={4}>Catalog</Typography.Title>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            placeholder="Search products"
            allowClear
            onSearch={(v) => {
              setPage(1);
              setSearch(v.trim());
            }}
            style={{ width: 280 }}
          />
          <Select
            allowClear
            placeholder="Category"
            options={categories}
            onChange={onCategoryChange}
            value={categoryId}
            style={{ minWidth: 200 }}
          />
          <Select
            placeholder="Sort by"
            allowClear
            options={[
              { label: 'Relevance', value: 'relevance' },
              { label: 'Price', value: 'price' },
              { label: 'Newest', value: 'createdAt' },
            ]}
            onChange={(v) => setSortBy(v)}
            style={{ width: 160 }}
          />
          <Select
            placeholder="Direction"
            allowClear
            options={[
              { label: 'Asc', value: 'asc' },
              { label: 'Desc', value: 'desc' },
            ]}
            onChange={(v) => setSortDirection(v)}
            style={{ width: 120 }}
          />
          <div style={{ width: 240 }}>
            <Typography.Text type="secondary">Price range</Typography.Text>
            <Slider
              range
              min={0}
              max={5000}
              step={10}
              value={priceRange ?? undefined}
              onChange={(v) => setPriceRange(v as [number, number])}
              onAfterChange={() => setPage(1)}
            />
          </div>
          <Select
            value={size}
            onChange={(v) => {
              setPage(1);
              setSize(v);
            }}
            options={PAGE_SIZE_OPTIONS.map((v) => ({ label: `${v} / page`, value: v }))}
            style={{ width: 140 }}
          />
        </Space>
      </Card>

      {loading ? (
        <Row gutter={[16, 16]}>
          {Array.from({ length: size }).map((_, i) => (
            <Col key={i} xs={24} sm={12} md={8} lg={6}>
              <Card>
                <Skeleton.Image style={{ width: '100%', height: 160 }} active />
                <Skeleton active paragraph={{ rows: 1 }} title />
              </Card>
            </Col>
          ))}
        </Row>
      ) : products.length === 0 ? (
        <Empty description="No products found" />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {products.map((p) => {
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

          <Divider />
          <Pagination
            current={page}
            pageSize={size}
            total={total}
            showSizeChanger={false}
            onChange={(cp) => setPage(cp)}
          />
        </>
      )}
    </div>
  );
}
