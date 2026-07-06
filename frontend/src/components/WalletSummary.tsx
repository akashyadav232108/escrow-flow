interface WalletSummaryProps {
  balance: number;
}

export default function WalletSummary({ balance }: WalletSummaryProps) {
  return (
    <div className="wallet-summary">
      <span className="wallet-summary-label">Current balance</span>
      <span className="wallet-summary-balance">
        ₹{balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
      </span>
    </div>
  );
}
