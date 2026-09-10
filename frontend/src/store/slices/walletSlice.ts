import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { walletApi } from '../../api/walletApi';
import type { Transaction } from '../../types';

interface WalletState {
  balance: number;
  transactions: Transaction[];
  transactionsPage: number;
  transactionsTotal: number;
  loading: boolean;
  error: string | null;
}

const initialState: WalletState = {
  balance: 0,
  transactions: [],
  transactionsPage: 0,
  transactionsTotal: 0,
  loading: false,
  error: null,
};

export const fetchWallet = createAsyncThunk('wallet/fetchWallet', async () => walletApi.getWallet());

export const fetchTransactions = createAsyncThunk(
  'wallet/fetchTransactions',
  async (params: { page?: number; size?: number } = {}) =>
    walletApi.getTransactions(params.page ?? 0, params.size ?? 20),
);

export const addFunds = createAsyncThunk('wallet/addFunds', async (amount: number) =>
  walletApi.addFunds(amount),
);

const walletSlice = createSlice({
  name: 'wallet',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchWallet.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchWallet.fulfilled, (state, action) => {
        state.loading = false;
        state.balance = action.payload.balance;
      })
      .addCase(fetchWallet.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message ?? 'Failed to load wallet';
      })
      .addCase(fetchTransactions.fulfilled, (state, action) => {
        state.transactions = action.payload.content;
        state.transactionsPage = action.payload.page;
        state.transactionsTotal = action.payload.totalElements;
      })
      .addCase(addFunds.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(addFunds.fulfilled, (state, action) => {
        state.loading = false;
        state.balance = action.payload.wallet.balance;
      })
      .addCase(addFunds.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message ?? 'Failed to add funds';
      });
  },
});

export default walletSlice.reducer;
