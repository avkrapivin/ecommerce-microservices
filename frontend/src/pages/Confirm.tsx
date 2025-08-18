import { Button, Card, Form, Input, Typography, message, Space } from 'antd';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { confirmEmail, resendConfirmationCode } from '../shared/api/auth';
import { useState } from 'react';

export function Confirm() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  
  // Get email from navigation state or query params
  const email = location.state?.email || new URLSearchParams(location.search).get('email');

  const onFinish = async (values: { confirmationCode: string }) => {
    if (!email) {
      message.error('Email not found. Please register again.');
      navigate('/auth/register');
      return;
    }

    setLoading(true);
    try {
      await confirmEmail(email, values.confirmationCode);
      message.success('Email confirmed successfully! You can now sign in.');
      navigate('/auth/login', { state: { email } });
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Invalid confirmation code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (!email) {
      message.error('Email not found. Please register again.');
      navigate('/auth/register');
      return;
    }

    setResending(true);
    try {
      await resendConfirmationCode(email);
      message.success('Confirmation code sent again. Please check your email.');
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Failed to resend code. Please try again.');
    } finally {
      setResending(false);
    }
  };

  if (!email) {
    return (
      <Card style={{ maxWidth: 420, margin: '0 auto' }}>
        <Typography.Title level={3} style={{ textAlign: 'center' }}>
          Email Not Found
        </Typography.Title>
        <Typography.Paragraph style={{ textAlign: 'center' }}>
          Please <Link to="/auth/register">register again</Link> to receive a confirmation code.
        </Typography.Paragraph>
      </Card>
    );
  }

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Confirm Your Email
      </Typography.Title>
      
      <Typography.Paragraph style={{ textAlign: 'center' }}>
        We've sent a confirmation code to <strong>{email}</strong>
      </Typography.Paragraph>
      
      <Form layout="vertical" form={form} onFinish={onFinish}>
        <Form.Item 
          label="Confirmation Code" 
          name="confirmationCode" 
          rules={[
            { required: true, message: 'Please enter the confirmation code' },
            { len: 6, message: 'Code must be 6 characters' }
          ]}
        >
          <Input 
            placeholder="Enter 6-digit code"
            maxLength={6}
            autoComplete="one-time-code"
          />
        </Form.Item>
        
        <Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            Confirm Email
          </Button>
        </Form.Item>
      </Form>
      
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Space direction="vertical" size="small">
          <Typography.Text>
            Didn't receive the code?{' '}
            <Button 
              type="link" 
              onClick={handleResendCode} 
              loading={resending}
              style={{ padding: 0, height: 'auto' }}
            >
              Resend Code
            </Button>
          </Typography.Text>
          <Typography.Text>
            <Link to="/auth/login">Back to Sign In</Link>
          </Typography.Text>
        </Space>
      </div>
    </Card>
  );
}
