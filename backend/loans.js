/**
 * Loan Recommendation Engine — Real Indian Fintech Products
 * Based on actual NBFC/Fintech partnerships and RBI-registered lenders.
 * Score thresholds derived from industry-standard CIBIL/Experian equivalents.
 */

const loanProducts = [
    // Tier 1 — Entry level (score 450+)
    {
        id: "L1",
        name: "KreditBee Instant Loan",
        provider: "Krazybee Services Pvt. Ltd. (NBFC)",
        minScore: 450,
        maxAmount: "₹2,00,000",
        interestRate: "16% – 29.95% p.a.",
        tenure: "3 – 24 months",
        link: "https://www.kreditbee.in/",
        tag: "Instant Disbursal",
        description: "Instant personal loan for salaried and self-employed with minimal documentation."
    },
    {
        id: "L2",
        name: "MoneyView Personal Loan",
        provider: "Whizdm Innovations Pvt. Ltd.",
        minScore: 500,
        maxAmount: "₹5,00,000",
        interestRate: "1.33% per month onwards",
        tenure: "3 – 60 months",
        link: "https://moneyview.in/",
        tag: "Low Credit Friendly",
        description: "Flexible personal loans for individuals with moderate credit scores using alternative data."
    },

    // Tier 2 — Mid-range (score 600+)
    {
        id: "L3",
        name: "Navi Instant Personal Loan",
        provider: "Navi Finserv Ltd. (RBI Registered NBFC)",
        minScore: 600,
        maxAmount: "₹20,00,000",
        interestRate: "9.9% p.a. onwards",
        tenure: "3 – 84 months",
        link: "https://navi.com/personal-loan/",
        tag: "App-Based",
        description: "100% digital loan process with competitive interest rates and instant approval."
    },
    {
        id: "L4",
        name: "Cashe Business Loan",
        provider: "Bhanix Finance & Investment Ltd.",
        minScore: 620,
        maxAmount: "₹4,00,000",
        interestRate: "2.75% per month",
        tenure: "3 – 18 months",
        link: "https://www.cashe.co.in/",
        tag: "For Self-Employed",
        description: "Short-tenure credit for small business owners and self-employed professionals."
    },

    // Tier 3 — Good credit (score 700+)
    {
        id: "L5",
        name: "Bajaj Finserv Personal Loan",
        provider: "Bajaj Finance Limited",
        minScore: 700,
        maxAmount: "₹40,00,000",
        interestRate: "11% p.a. onwards",
        tenure: "12 – 96 months",
        link: "https://www.bajajfinserv.in/personal-loan",
        tag: "Top Pick",
        description: "One of India's largest NBFCs. Pre-approved offers, same-day disbursal for eligible customers."
    },
    {
        id: "L6",
        name: "HDFC Bank SmartLoan",
        provider: "HDFC Bank Ltd.",
        minScore: 720,
        maxAmount: "₹75,00,000",
        interestRate: "10.85% p.a. onwards",
        tenure: "12 – 60 months",
        link: "https://www.hdfcbank.com/personal/borrow/popular-loans/personal-loan",
        tag: "Bank Backed",
        description: "Paperless pre-approved personal loans for existing HDFC customers with strong profiles."
    },

    // Tier 4 — Premium (score 750+)
    {
        id: "L7",
        name: "Axis Bank Credit Line",
        provider: "Axis Bank Ltd.",
        minScore: 750,
        maxAmount: "₹1,50,00,000",
        interestRate: "10.49% p.a. onwards",
        tenure: "12 – 84 months",
        link: "https://www.axisbank.com/retail/loans/personal-loan",
        tag: "Premium",
        description: "High-value credit for premium borrowers with low risk profiles and strong credit history."
    },
    {
        id: "L8",
        name: "ICICI Bank Insta Loan",
        provider: "ICICI Bank Ltd.",
        minScore: 780,
        maxAmount: "₹50,00,000",
        interestRate: "10.75% p.a. onwards",
        tenure: "12 – 72 months",
        link: "https://www.icicibank.com/personal-banking/loans/personal-loan",
        tag: "Instant Approval",
        description: "Pre-approved offers for top-scoring applicants. Approved and disbursed within hours."
    }
];

/**
 * Returns all products with eligibility state, sorted by eligibility then score.
 * Eligible products are shown first, locked ones show how many points are needed.
 */
function getLoans(score) {
    const scored = loanProducts.map(loan => {
        const isEligible = score >= loan.minScore;
        return {
            ...loan,
            eligible: isEligible,
            pointsToUnlock: isEligible ? 0 : (loan.minScore - score)
        };
    });

    // Sort: eligible first, then by minScore ascending
    return scored.sort((a, b) => {
        if (a.eligible && !b.eligible) return -1;
        if (!a.eligible && b.eligible) return 1;
        return a.minScore - b.minScore;
    });
}

module.exports = { getLoans };
