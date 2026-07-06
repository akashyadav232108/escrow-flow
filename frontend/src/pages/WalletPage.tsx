import { useEffect, useState, type FormEvent } from 'react';
import TransactionHistory from '../components/TransactionHistory';
import WalletSummary from '../components/WalletSummary';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { addFunds, fetchTransactions, fetchWallet } from '../store/slices/walletSlice';

export default function WalletPage() {
  const dispatch = useAppDispatch();
  const { balance, transactions, loading, error } = useAppSelector((state) => state.wallet);
  const [amount, setAmount] = useState('');
  const [showAddFunds, setShowAddFunds] = useState(false);

  useEffect(() => {
    dispatch(fetchWallet());
    dispatch(fetchTransactions({}));
  }, [dispatch]);

  const handleAddFunds = async (event: FormEvent) => {
    event.preventDefault();
    const numericAmount = Number(amount);
    if (!numericAmount || numericAmount <= 0) return;
    await dispatch(addFunds(numericAmount));
    setAmount('');
    setShowAddFunds(false);
    dispatch(fetchTransactions({}));
  };

  return (
    <div className="wallet-page">
      <WalletSummary balance={balance} />
      <button
        type="button"
        className={showAddFunds ? 'btn-secondary' : 'btn-primary'}
        onClick={() => setShowAddFunds((prev) => !prev)}
      >
        {showAddFunds ? 'Cancel' : 'Add funds'}
      </button>
      {showAddFunds && (
        <form className="add-funds-form" onSubmit={handleAddFunds}>
          <input
            type="number"
            min="1"
            step="0.01"
            placeholder="Amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
          <button type="submit" className="btn-primary" disabled={loading}>
            Confirm
          </button>
        </form>
      )}
      {error && <p className="error-text">{error}</p>}
      <h2>Transaction history</h2>
      <TransactionHistory transactions={transactions} />
    </div>
  );
}
