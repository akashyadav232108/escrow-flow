import type { Transaction } from '../types';

export default function TransactionHistory({ transactions }: { transactions: Transaction[] }) {
  if (transactions.length === 0) {
    return <p className="empty-state">No transactions yet. Add funds to get started!</p>;
  }

  const getAmountClass = (type: string) => {
    return type === 'CREDIT' ? 'transaction-amount-positive' : 'transaction-amount-negative';
  };

  const getAmountPrefix = (type: string) => {
    return type === 'CREDIT' ? '+' : '-';
  };

  const formatType = (type: string) => {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (l) => l.toUpperCase());
  };

  return (
    <div className="table-scroll">
      <table className="transaction-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Reference</th>
            <th>Balance After</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx, index) => (
            <tr key={tx.id} style={{ animationDelay: `${index * 0.05}s` }}>
              <td>{new Date(tx.createdAt).toLocaleString()}</td>
              <td>{formatType(tx.type)}</td>
              <td className={getAmountClass(tx.type)}>
                {getAmountPrefix(tx.type)}₹{Math.abs(tx.amount).toLocaleString('en-IN', {
                  minimumFractionDigits: 2,
                })}
              </td>
              <td>
                {tx.referenceType} #{tx.referenceId}
              </td>
              <td>₹{tx.balanceAfter.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
