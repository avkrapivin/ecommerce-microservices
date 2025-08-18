import { Button, Card, Form, Input, Typography, message, Space } from 'antd';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { login } from '../shared/api/auth';
import { useState } from 'react';

export function Login() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  
  // Pre-fill email if coming from registration or password reset
  const email = location.state?.email || '';

  const onFinish = async (values: { email: string; password: string }) => {
    setLoading(true);
    try {
      await login(values);
      message.success('Signed in successfully');
      window.location.href = '/';
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Failed to sign in');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Sign In
      </Typography.Title>
      
      <Form layout="vertical" form={form} onFinish={onFinish} initialValues={{ email }}>
        <Form.Item 
          label="Email" 
          name="email" 
          rules={[
            { required: true, message: 'Please enter your email' },
            { type: 'email', message: 'Please enter a valid email' }
          ]}
        >
          <Input autoComplete="email" />
        </Form.Item>
        
        <Form.Item 
          label="Password" 
          name="password" 
          rules={[{ required: true, message: 'Please enter your password' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        
        <Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            Sign In
          </Button>
        </Form.Item>
      </Form>
      
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Space direction="vertical" size="small">
          <Typography.Text>
            Don't have an account? <Link to="/auth/register">Create one</Link>
          </Typography.Text>
          <Typography.Text>
            <Link to="/auth/forgot">Forgot your password?</Link>
          </Typography.Text>
        </Space>
      </div>
    </Card>
  );
}
