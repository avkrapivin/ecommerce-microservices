import { Button, Card, Form, Input, Typography, message, Space, Steps } from 'antd';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { resetPassword, confirmPasswordReset } from '../shared/api/auth';
import { useState } from 'react';

const { Step } = Steps;

export function ForgotPassword() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [email, setEmail] = useState(location.state?.email || '');
  const [resetCode, setResetCode] = useState('');

  const onResetPassword = async (values: { email: string }) => {
    setLoading(true);
    try {
      await resetPassword(values.email);
      setEmail(values.email);
      setCurrentStep(1);
      message.success('Reset code sent to your email. Please check your inbox.');
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Failed to send reset code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const onConfirmReset = async (values: { code: string; newPassword: string }) => {
    if (!email) {
      message.error('Email not found. Please start over.');
      setCurrentStep(0);
      return;
    }

    setLoading(true);
    try {
      await confirmPasswordReset(email, values.code, values.newPassword);
      message.success('Password reset successfully! You can now sign in with your new password.');
      navigate('/auth/login', { state: { email } });
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Invalid reset code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <Form layout="vertical" form={form} onFinish={onResetPassword}>
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
            
            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={loading}>
                Send Reset Code
              </Button>
            </Form.Item>
          </Form>
        );

      case 1:
        return (
          <Form layout="vertical" onFinish={onConfirmReset}>
            <Form.Item 
              label="Reset Code" 
              name="code" 
              rules={[
                { required: true, message: 'Please enter the reset code' },
                { len: 6, message: 'Code must be 6 characters' }
              ]}
            >
              <Input 
                placeholder="Enter 6-digit code"
                maxLength={6}
                autoComplete="one-time-code"
              />
            </Form.Item>
            
            <Form.Item 
              label="New Password" 
              name="newPassword" 
              rules={[
                { required: true, message: 'Please enter your new password' },
                { min: 6, message: 'Password must be at least 6 characters' }
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            
            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={loading}>
                Reset Password
              </Button>
            </Form.Item>
          </Form>
        );

      default:
        return null;
    }
  };

  return (
    <Card style={{ maxWidth: 420, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        Reset Password
      </Typography.Title>
      
      <Steps current={currentStep} size="small" style={{ marginBottom: 24 }}>
        <Step title="Enter Email" />
        <Step title="Reset Password" />
      </Steps>
      
      {renderStepContent()}
      
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Space direction="vertical" size="small">
          <Typography.Text>
            Remember your password? <Link to="/auth/login">Sign in</Link>
          </Typography.Text>
          <Typography.Text>
            Don't have an account? <Link to="/auth/register">Register</Link>
          </Typography.Text>
          {currentStep === 1 && (
            <Typography.Text>
              <Button 
                type="link" 
                onClick={() => setCurrentStep(0)}
                style={{ padding: 0, height: 'auto' }}
              >
                Back to Email
              </Button>
            </Typography.Text>
          )}
        </Space>
      </div>
    </Card>
  );
}
