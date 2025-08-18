import { Card, Typography, Form, Input, Button, Space, message, Divider, Skeleton } from 'antd';
import { useState, useEffect } from 'react';
import { getProfile, updateProfile, logout } from '../shared/api/auth';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;

export function Profile() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [user, setUser] = useState<{
    email: string;
    firstName?: string;
    lastName?: string;
  } | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const profile = await getProfile();
      setUser(profile);
      form.setFieldsValue({
        firstName: profile.firstName || '',
        lastName: profile.lastName || ''
      });
    } catch (err: any) {
      message.error('Failed to load profile');
      if (err?.response?.status === 401) {
        navigate('/auth/login');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async (values: { firstName: string; lastName: string }) => {
    setUpdating(true);
    try {
      await updateProfile(values);
      setUser(prev => prev ? { ...prev, ...values } : null);
      setIsEditing(false);
      message.success('Profile updated successfully');
    } catch (err: any) {
      message.error(err?.response?.data?.message ?? 'Failed to update profile');
    } finally {
      setUpdating(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      message.success('Logged out successfully');
      navigate('/');
    } catch (err: any) {
      message.error('Failed to logout');
    }
  };

  if (loading) {
    return (
      <Card>
        <Skeleton active />
      </Card>
    );
  }

  if (!user) {
    return (
      <Card>
        <Title level={4}>Profile Not Found</Title>
        <Text>Unable to load user profile.</Text>
      </Card>
    );
  }

  return (
    <Card>
      <Title level={3}>Profile</Title>
      
      <div style={{ marginBottom: 24 }}>
        <Space direction="vertical" size="small">
          <div>
            <Text strong>Email:</Text> <Text>{user.email}</Text>
          </div>
          <div>
            <Text strong>First Name:</Text> <Text>{user.firstName || 'Not set'}</Text>
          </div>
          <div>
            <Text strong>Last Name:</Text> <Text>{user.lastName || 'Not set'}</Text>
          </div>
        </Space>
      </div>

      <Divider />

      {!isEditing ? (
        <Space>
          <Button type="primary" onClick={() => setIsEditing(true)}>
            Edit Profile
          </Button>
          <Button onClick={handleLogout}>
            Logout
          </Button>
        </Space>
      ) : (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleUpdateProfile}
          style={{ maxWidth: 400 }}
        >
          <Form.Item
            label="First Name"
            name="firstName"
            rules={[{ required: true, message: 'Please enter your first name' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="Last Name"
            name="lastName"
            rules={[{ required: true, message: 'Please enter your last name' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={updating}>
                Save Changes
              </Button>
              <Button onClick={() => setIsEditing(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      )}
    </Card>
  );
}
