import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { AppLayout } from '../../widgets/layouts/AppLayout';
import { Home } from '../../pages/Home';
import { Catalog } from '../../pages/Catalog';
import { Product } from '../../pages/Product';
import { Login } from '../../pages/Login';
import { Register } from '../../pages/Register';
import { Confirm } from '../../pages/Confirm';
import { ForgotPassword } from '../../pages/ForgotPassword';
import { Profile } from '../../pages/Profile';
import { CartPage } from '../../pages/CartPage';
import { NotFound } from '../../pages/NotFound';
import { PrivateRoute } from './PrivateRoute';

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
      { path: 'auth/confirm', element: <Confirm /> },
      { path: 'auth/forgot', element: <ForgotPassword /> },
      { path: 'profile', element: (
        <PrivateRoute>
          <Profile />
        </PrivateRoute>
      ) },
      { path: 'cart', element: <CartPage /> },
      { path: '*', element: <NotFound /> },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
