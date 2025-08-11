import { Button, Card, Form, Input, Typography } from 'antd';

export function Register() {
  const onFinish = async (values: { email: string; password: string; givenName?: string; familyName?: string }) => {
    // TODO: implement registration flow
    console.log(values);
  };

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Register
      </Typography.Title>
      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item label="Email" name="email" rules={[{ required: true, type: 'email' }]}>
          <Input autoComplete="email" />
        </Form.Item>
        <Form.Item label="Password" name="password" rules={[{ required: true, min: 6 }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            Create account
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
