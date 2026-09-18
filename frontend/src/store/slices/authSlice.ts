import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import {
  authApi,
  type ChangePasswordPayload,
  type ForgotPasswordPayload,
  type LoginPayload,
  type ResendOtpPayload,
  type ResetPasswordPayload,
  type SignupPayload,
  type VerifyEmailPayload,
} from '../../api/authApi';
import type { User } from '../../types';
import { extractApiErrorMessage } from '../../utils/errors';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

const storedToken = localStorage.getItem('token');
const storedUser = localStorage.getItem('user');

const initialState: AuthState = {
  user: storedUser ? (JSON.parse(storedUser) as User) : null,
  token: storedToken,
  isAuthenticated: Boolean(storedToken),
  loading: false,
  error: null,
};

export const signup = createAsyncThunk(
  'auth/signup',
  async (payload: SignupPayload, { rejectWithValue }) => {
    try {
      await authApi.signup(payload);
      return payload.email; // Return email for navigation
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Signup failed'));
    }
  },
);

export const verifyEmail = createAsyncThunk(
  'auth/verifyEmail',
  async (payload: VerifyEmailPayload, { rejectWithValue }) => {
    try {
      return await authApi.verifyEmail(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Verification failed'));
    }
  },
);

export const resendOtp = createAsyncThunk(
  'auth/resendOtp',
  async (payload: ResendOtpPayload, { rejectWithValue }) => {
    try {
      await authApi.resendOtp(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to resend code'));
    }
  },
);

export const login = createAsyncThunk(
  'auth/login',
  async (payload: LoginPayload, { rejectWithValue }) => {
    try {
      return await authApi.login(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Login failed'));
    }
  },
);

export const forgotPassword = createAsyncThunk(
  'auth/forgotPassword',
  async (payload: ForgotPasswordPayload, { rejectWithValue }) => {
    try {
      await authApi.forgotPassword(payload);
      return payload.email;
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to send reset code'));
    }
  },
);

export const resetPassword = createAsyncThunk(
  'auth/resetPassword',
  async (payload: ResetPasswordPayload, { rejectWithValue }) => {
    try {
      await authApi.resetPassword(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to reset password'));
    }
  },
);

export const changePassword = createAsyncThunk(
  'auth/changePassword',
  async (payload: ChangePasswordPayload, { rejectWithValue }) => {
    try {
      await authApi.changePassword(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to change password'));
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    },
  },
  extraReducers: (builder) => {
    builder
      // Signup - no longer logs in immediately
      .addCase(signup.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(signup.fulfilled, (state) => {
        state.loading = false;
        // Don't set user/token - must verify email first
      })
      .addCase(signup.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Signup failed';
      })
      // Verify email - logs in after verification
      .addCase(verifyEmail.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(verifyEmail.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.isAuthenticated = true;
        localStorage.setItem('token', action.payload.token);
        localStorage.setItem('user', JSON.stringify(action.payload.user));
      })
      .addCase(verifyEmail.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Verification failed';
      })
      // Resend OTP
      .addCase(resendOtp.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(resendOtp.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(resendOtp.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Failed to resend code';
      })
      // Login
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.isAuthenticated = true;
        localStorage.setItem('token', action.payload.token);
        localStorage.setItem('user', JSON.stringify(action.payload.user));
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Login failed';
      })
      // Forgot password
      .addCase(forgotPassword.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(forgotPassword.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(forgotPassword.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Failed to send reset code';
      })
      // Reset password
      .addCase(resetPassword.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(resetPassword.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(resetPassword.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) ?? action.error.message ?? 'Failed to reset password';
      });
  },
});

export const { logout } = authSlice.actions;
export default authSlice.reducer;
