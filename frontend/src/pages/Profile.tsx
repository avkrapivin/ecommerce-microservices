import { Card, Typography } from 'antd';

export function Profile() {
  return (
    <Card>
      <Typography.Title level={4}>Profile</Typography.Title>
      <Typography.Paragraph>User info will be shown here.</Typography.Paragraph>
    </Card>
  );
}
