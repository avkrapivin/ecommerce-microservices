import { Button, Card, Form, Input, Typography, message } from 'antd';
import { login } from '../shared/api/auth';

export function Login() {
  const [form] = Form.useForm();

  const onFinish = async (values: { email: string; password: string }) => {
    try {
      await login(values);
      message.success('Signed in successfully');
      window.location.href = '/';
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Failed to sign in');
    }
  };

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Sign in
      </Typography.Title>
      <Form layout="vertical" form={form} onFinish={onFinish}>
        <Form.Item label="Email" name="email" rules={[{ required: true, type: 'email' }]}>
          <Input autoComplete="email" />
        </Form.Item>
        <Form.Item label="Password" name="password" rules={[{ required: true }]}>
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            Sign in
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
