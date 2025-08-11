import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { AppLayout } from '../../widgets/layouts/AppLayout';
import { Home } from '../../pages/Home';
import { Catalog } from '../../pages/Catalog';
import { Product } from '../../pages/Product';
import { Login } from '../../pages/Login';
import { Register } from '../../pages/Register';
import { Profile } from '../../pages/Profile';
import { NotFound } from '../../pages/NotFound';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'catalog', element: <Catalog /> },
      { path: 'product/:id', element: <Product /> },
      { path: 'auth/login', element: <Login /> },
      { path: 'auth/register', element: <Register /> },
      { path: 'profile', element: <Profile /> },
      { path: '*', element: <NotFound /> },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
