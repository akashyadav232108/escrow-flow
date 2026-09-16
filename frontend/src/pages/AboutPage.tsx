export default function AboutPage() {
  return (
    <div className="about-page">
      <h1>About Escrow Flow</h1>
      
      <section className="about-section">
        <h2>Our Mission</h2>
        <p>
          Escrow Flow is a trusted platform that provides secure payment solutions for freelancers 
          and clients. We ensure that both parties are protected throughout the project lifecycle 
          by holding funds in escrow until work is completed and approved.
        </p>
      </section>

      <section className="about-section">
        <h2>How It Works</h2>
        <div className="about-steps">
          <div className="about-step">
            <h3>1. Create a Project</h3>
            <p>Clients post projects with clear milestones and payment amounts.</p>
          </div>
          <div className="about-step">
            <h3>2. Accept Agreement</h3>
            <p>Both parties review and accept project terms before work begins.</p>
          </div>
          <div className="about-step">
            <h3>3. Lock Funds in Escrow</h3>
            <p>Client funds are securely held in escrow until milestone completion.</p>
          </div>
          <div className="about-step">
            <h3>4. Complete & Get Paid</h3>
            <p>Once work is approved, funds are released to the freelancer instantly.</p>
          </div>
        </div>
      </section>

      <section className="about-section">
        <h2>Secure Project Agreements</h2>
        <p>
          Every project on Escrow Flow requires a formal agreement to protect both parties. 
          Before any work begins or funds are locked, both the client and freelancer must 
          explicitly accept the project terms.
        </p>
        <ul className="about-features">
          <li><strong>Clear Terms:</strong> Detailed milestones, deliverables, and payment amounts.</li>
          <li><strong>Mutual Consent:</strong> Both parties must accept before project activation.</li>
          <li><strong>Legal Protection:</strong> Binding agreement protects your interests.</li>
          <li><strong>Full Transparency:</strong> All terms visible to both parties from the start.</li>
        </ul>
      </section>

      <section className="about-section">
        <h2>Fair Dispute Resolution</h2>
        <p>
          Sometimes projects don't go as planned. Our comprehensive dispute resolution system 
          ensures fair outcomes for all parties.
        </p>
        <div className="about-steps">
          <div className="about-step">
            <h3>1. Raise a Dispute</h3>
            <p>Either client or freelancer can raise a dispute on any milestone if there's disagreement.</p>
          </div>
          <div className="about-step">
            <h3>2. Admin Review</h3>
            <p>Independent admin reviews evidence, work delivered, and agreement terms from both sides.</p>
          </div>
          <div className="about-step">
            <h3>3. Fair Resolution</h3>
            <p>Admin makes an evidence-based decision, and escrow funds are distributed accordingly.</p>
          </div>
        </div>
        <p style={{ marginTop: '1.5rem', color: 'var(--color-text-muted)' }}>
          Your funds remain secure in escrow throughout the dispute process, and all decisions 
          are made transparently based on the original project agreement.
        </p>
      </section>

      <section className="about-section">
        <h2>Why Choose Escrow Flow?</h2>
        <ul className="about-features">
          <li><strong>Secure Payments:</strong> Your funds are protected with industry-standard security and held in escrow.</li>
          <li><strong>Binding Agreements:</strong> Formal agreements protect both clients and freelancers before work begins.</li>
          <li><strong>Transparent Process:</strong> Clear milestone tracking, status updates, and complete audit trail.</li>
          <li><strong>Fair Dispute Resolution:</strong> Independent admin mediation ensures fair outcomes for all parties.</li>
          <li><strong>Fast Withdrawals:</strong> Get paid immediately when work is approved—no delays.</li>
          <li><strong>Complete Protection:</strong> Both parties are protected at every stage of the project lifecycle.</li>
        </ul>
      </section>

      <section className="about-section">
        <h2>Get Started</h2>
        <p>
          Ready to experience secure, protected project payments? Browse available projects 
          or create your own today. Every project includes formal agreements and dispute 
          protection for your peace of mind.
        </p>
      </section>
    </div>
  );
}
