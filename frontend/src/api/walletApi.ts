import api from './client';
import type { TransactionPage, Wallet } from '../types';

export const walletApi = {
  getWallet: () => api.get<Wallet>('/wallet').then((res) => res.data),

  addFunds: (amount: number) =>
    api.post<Wallet>('/wallet/add-funds', { amount }).then((res) => res.data),

  getTransactions: (page = 0, size = 20) =>
    api
      .get<TransactionPage>('/wallet/transactions', { params: { page, size } })
      .then((res) => res.data),
};
