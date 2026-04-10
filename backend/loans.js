/**
 * Loan Recommendation Engine
 */

const loanProducts = [
    {
        id: "L1",
        name: "Micro Business Loan",
        provider: "Demo Bank",
        minScore: 650,
        amount: "₹10,000 - ₹50,000",
        link: "https://example.com/apply-micro"
    },
    {
        id: "L2",
        name: "Starter Loan",
        provider: "Fintech Partner",
        minScore: 500,
        amount: "₹5,000 - ₹20,000",
        link: "https://example.com/apply-starter"
    },
    {
        id: "L3",
        name: "Growth Capital",
        provider: "SME Credit",
        minScore: 750,
        amount: "₹1,00,000 - ₹5,00,000",
        link: "https://example.com/apply-growth"
    }
];

/**
 * Filter available loans based on credit score
 */
function getEligibleLoans(score) {
    return loanProducts.filter(loan => score >= loan.minScore);
}

module.exports = { getEligibleLoans };
