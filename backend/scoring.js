/**
 * Scoring Module
 * Implements additive, explainable scoring.
 * Formula: Score = 300 (Base) + Income + Activity + Stability
 */

function calculateScore(features) {
    const baseScore = 300;
    
    // 1. Income Strength (Max 250 points)
    // Goal: 20k+ income for full points
    const incomePoints = Math.round(Math.min(features.totalCredit / 20000, 1) * 250);
    
    // 2. Business Activity (Max 150 points)
    // Goal: 30+ transactions for full points
    const activityPoints = Math.round(Math.min(features.transactionCount / 30, 1) * 150);

    // 3. Stability & Cash Flow (Max 200 points)
    let stabilityPoints = 0;
    const ratio = features.spendingRatio;
    if (ratio < 0.4) stabilityPoints = 200;
    else if (ratio < 0.6) stabilityPoints = 150;
    else if (ratio < 0.8) stabilityPoints = 100;
    else stabilityPoints = 50;

    const total = baseScore + incomePoints + activityPoints + stabilityPoints;

    return {
        total: Math.min(total, 900),
        breakdown: {
            base: baseScore,
            income: incomePoints,
            activity: activityPoints,
            stability: stabilityPoints
        }
    };
}

function classifyRisk(score) {
    if (score >= 750) return "LOW";
    if (score >= 550) return "MEDIUM";
    return "HIGH";
}

function generateInsights(features) {
    const insights = {
        income_strength: "Insufficient data",
        spending_behavior: "Insufficient data",
        activity_level: "Insufficient data"
    };

    if (features.totalCredit > 20000) insights.income_strength = "Strong consistent cash inflow.";
    else if (features.totalCredit > 5000) insights.income_strength = "Steady income flow detected.";
    else insights.income_strength = "Low business inflow identified.";

    if (features.spendingRatio < 0.5) insights.spending_behavior = "Excellent overhead management.";
    else if (features.spendingRatio < 0.8) insights.spending_behavior = "Sustainable spending patterns.";
    else insights.spending_behavior = "High outflow relative to income.";

    if (features.transactionCount > 25) insights.activity_level = "High-velocity business activity.";
    else if (features.transactionCount > 10) insights.activity_level = "Regular business transactions.";
    else insights.activity_level = "Limited business interaction.";

    return insights;
}

function generateSummary(features, score) {
    if (features.transactionCount === 0) return "Gather more business activity to generate a profile.";
    if (score >= 750) return "Premium merchant profile with high loan eligibility.";
    if (score >= 550) return "Solid business standing with moderate credit capacity.";
    return "Action required: Increase inflow to improve creditworthiness.";
}

module.exports = { 
    calculateScore, 
    classifyRisk, 
    generateInsights, 
    generateSummary 
};
