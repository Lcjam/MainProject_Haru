import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';

interface PrivateRouteProps {
  children: React.ReactNode;
}

const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const { token } = useAppSelector((state) => state.auth);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
