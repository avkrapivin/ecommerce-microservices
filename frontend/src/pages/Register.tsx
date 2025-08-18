import { Button, Card, Form, Input, Typography, message, Space } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../shared/api/auth';
import { useState } from 'react';

export function Register() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { email: string; password: string; firstName: string; lastName: string }) => {
    setLoading(true);
    try {
      await register(values);
      message.success('Registration successful! Please check your email for confirmation code.');
      // Redirect to confirmation page with email
      navigate('/auth/confirm', { state: { email: values.email } });
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Create Account
      </Typography.Title>
      
      <Form layout="vertical" form={form} onFinish={onFinish}>
        <Form.Item 
          label="First Name" 
          name="firstName" 
          rules={[{ required: true, message: 'Please enter your first name' }]}
        >
          <Input autoComplete="given-name" />
        </Form.Item>
        
        <Form.Item 
          label="Last Name" 
          name="lastName" 
          rules={[{ required: true, message: 'Please enter your last name' }]}
        >
          <Input autoComplete="family-name" />
        </Form.Item>
        
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
          rules={[
            { required: true, message: 'Please enter your password' },
            { min: 6, message: 'Password must be at least 6 characters' }
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        
        <Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            Create Account
          </Button>
        </Form.Item>
      </Form>
      
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Space direction="vertical" size="small">
          <Typography.Text>
            Already have an account? <Link to="/auth/login">Sign in</Link>
          </Typography.Text>
          <Typography.Text>
            <Link to="/auth/forgot">Forgot your password?</Link>
          </Typography.Text>
        </Space>
      </div>
    </Card>
  );
}
