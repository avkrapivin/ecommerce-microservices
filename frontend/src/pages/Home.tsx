import { Card, Typography } from 'antd';

export function Home() {
  return (
    <Card>
      <Typography.Title level={3}>Welcome to the store</Typography.Title>
      <Typography.Paragraph>
        Browse our catalog, sign in to see your profile, and place orders.
      </Typography.Paragraph>
    </Card>
  );
}
