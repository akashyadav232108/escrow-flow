import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';

interface ProtectedActionOptions {
  action?: string;
  message?: string;
}

/**
 * Hook that wraps an action requiring authentication.
 * If the user is not authenticated, they are redirected to signup
 * with the option to go back or complete registration.
 */
export function useProtectedAction() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  const navigate = useNavigate();
  const location = useLocation();

  const wrapAction = useCallback(
    <T extends unknown[]>(
      action: (...args: T) => void | Promise<void>,
      options: ProtectedActionOptions = {},
    ) => {
      return (...args: T) => {
        if (!isAuthenticated) {
          navigate('/signup', {
            state: {
              from: location,
              action: options.action,
              message: options.message || 'Please sign up or log in to continue',
            },
          });
          return;
        }
        return action(...args);
      };
    },
    [isAuthenticated, navigate, location],
  );

  return { wrapAction, isAuthenticated };
}
