/**
 * Feature Engineering Module
 * Computes financial metrics and identifies spending patterns.
 */

function calculateFeatures(transactions) {
    if (!transactions || transactions.length === 0) {
        return {
            totalCredit: 0,
            totalDebit: 0,
            transactionCount: 0,
            avgTransactionValue: 0,
            spendingRatio: 0,
            netBalance: 0,
            topMerchants: []
        };
    }

    let totalCredit = 0;
    let totalDebit = 0;
    const merchants = {};

    transactions
        .filter(tx => tx.amount && tx.amount > 0)
        .forEach(tx => {
            const typeLower = (tx.type || "").toLowerCase().trim();
            const amount = parseFloat(tx.amount || 0);
            
            if (typeLower === 'credit') {
                totalCredit += amount;
            } else if (typeLower === 'debit') {
                totalDebit += amount;
            }

            // Count merchant frequency
            if (tx.merchant && tx.merchant !== 'Unknown') {
                merchants[tx.merchant] = (merchants[tx.merchant] || 0) + 1;
            }
        });

    const filteredTransactions = transactions.filter(tx => tx.amount && tx.amount > 0);
    const transactionCount = filteredTransactions.length;
    const avgTransactionValue = transactionCount > 0 ? (totalCredit + totalDebit) / transactionCount : 0;
    
    // Calculate spending ratio (capped at 2.0)
    let spendingRatio = totalCredit > 0 ? totalDebit / totalCredit : (totalDebit > 0 ? 2 : 0);
    spendingRatio = Math.min(spendingRatio, 2);

    const netBalance = totalCredit - totalDebit;

    // Get top 3 merchants
    const topMerchants = Object.entries(merchants)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(entry => entry[0]);

    return {
        totalCredit,
        totalDebit,
        transactionCount,
        avgTransactionValue: Math.round(avgTransactionValue * 100) / 100,
        spendingRatio: Math.round(spendingRatio * 100) / 100,
        netBalance,
        topMerchants
    };
}

module.exports = { calculateFeatures };
