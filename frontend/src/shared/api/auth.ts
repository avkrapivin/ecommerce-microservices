import { http } from './http';

export interface LoginDto {
  email: string;
  password: string;
}

export async function login(dto: LoginDto) {
  // Server will set HttpOnly cookies via Set-Cookie headers
  const { data } = await http.post('/users/login', dto);
  return data;
}

export async function getProfile() {
  const { data } = await http.get('/users/profile');
  return data as {
    email: string;
    firstName?: string;
    lastName?: string;
  };
}

export async function logout() {
  await http.post('/users/logout');
}
