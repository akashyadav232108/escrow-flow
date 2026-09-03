import api from './client';
import type { AddFundsResponse, TransactionPage, Wallet } from '../types';

export const walletApi = {
  getWallet: () => api.get<Wallet>('/wallet').then((res) => res.data),

  addFunds: (amount: number) =>
    api.post<AddFundsResponse>('/wallet/add-funds', { amount }).then((res) => res.data),

  getTransactions: (page = 0, size = 20) =>
    api
      .get<TransactionPage>('/wallet/transactions', { params: { page, size } })
      .then((res) => res.data),
};
