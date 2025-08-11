import { Card, Typography } from 'antd';
import { useParams } from 'react-router-dom';

export function Product() {
  const { id } = useParams();
  return (
    <Card>
      <Typography.Title level={4}>Product {id}</Typography.Title>
      <Typography.Paragraph>Product details will be shown here.</Typography.Paragraph>
    </Card>
  );
}
