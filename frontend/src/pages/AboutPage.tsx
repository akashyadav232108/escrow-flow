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
            <h3>2. Lock Funds in Escrow</h3>
            <p>Client funds are securely held in escrow until milestone completion.</p>
          </div>
          <div className="about-step">
            <h3>3. Complete & Get Paid</h3>
            <p>Once work is approved, funds are released to the freelancer instantly.</p>
          </div>
        </div>
      </section>

      <section className="about-section">
        <h2>Why Choose Escrow Flow?</h2>
        <ul className="about-features">
          <li><strong>Secure Payments:</strong> Your funds are protected with industry-standard security.</li>
          <li><strong>Transparent Process:</strong> Clear milestone tracking and status updates.</li>
          <li><strong>Dispute Resolution:</strong> Built-in dispute handling for peace of mind.</li>
          <li><strong>Fast Withdrawals:</strong> Get paid immediately when work is approved.</li>
        </ul>
      </section>

      <section className="about-section">
        <h2>Get Started</h2>
        <p>
          Ready to experience secure project payments? Browse available projects or create your own today.
        </p>
      </section>
    </div>
  );
}
