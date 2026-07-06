import type { Transaction } from '../types';

export default function TransactionHistory({ transactions }: { transactions: Transaction[] }) {
  if (transactions.length === 0) {
    return <p>No transactions yet.</p>;
  }

  return (
    <table className="transaction-table">
      <thead>
        <tr>
          <th>Date</th>
          <th>Type</th>
          <th>Amount</th>
          <th>Reference</th>
          <th>Balance after</th>
        </tr>
      </thead>
      <tbody>
        {transactions.map((tx) => (
          <tr key={tx.id}>
            <td>{new Date(tx.createdAt).toLocaleString()}</td>
            <td>{tx.type}</td>
            <td>₹{tx.amount}</td>
            <td>
              {tx.referenceType} #{tx.referenceId}
            </td>
            <td>₹{tx.balanceAfter}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
