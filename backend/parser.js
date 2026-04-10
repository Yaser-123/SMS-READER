/**
 * SMS Parser utility
 * Extracts Amount, Merchant, Type, and Date from UPI messages.
 */

function parseUPIMessage(body) {
    if (!body) return null;

    const data = {
        amount: null,
        type: null,
        merchant: "Unknown",
        date: "Unknown",
        confidence: "low"
    };

    // 1. Normalize and Extract Amount
    // Improved Regex: Supports Rs., ₹, INR with proper dot escaping
    const amountRegex = /(Rs\.?|₹|INR)\s?([\d,]+(?:\.\d+)?)/i;
    const amountMatch = body.match(amountRegex);
    
    if (amountMatch) {
        // Remove commas and parse to float
        const rawAmount = amountMatch[2].replace(/,/g, '');
        const amount = parseFloat(rawAmount);

        // STRICT VALIDATION: Return null if invalid, zero, or negative
        if (!amount || isNaN(amount) || amount <= 0) {
            return null;
        }
        data.amount = amount;
    } else {
        return null;
    }

    // 2. Detect Transaction Type
    const bodyLower = body.toLowerCase();
    if (bodyLower.includes("debited") || bodyLower.includes("paid to") || bodyLower.includes("sent to")) {
        data.type = "debit";
    } else if (bodyLower.includes("credited") || bodyLower.includes("received from") || bodyLower.includes("received in")) {
        data.type = "credit";
    } else {
        return null; 
    }

    // 3. Extract Merchant Name
    const merchantRegex = /(?:to|from|at)\s+(.*?)(?:\s+via|\son|\sRef|$|\.|\sis)/i;
    const merchantMatch = body.match(merchantRegex);
    if (merchantMatch) {
        data.merchant = merchantMatch[1].trim();
    }

    // 4. Extract Date
    const dateRegex = /(?:on\s)(\d{2}[a-zA-Z]{3}\d{2})/i;
    const dateMatch = body.match(dateRegex);
    if (dateMatch) {
        data.date = dateMatch[1];
    } else {
        const simpleDateRegex = /(\d{2}[-/]\d{2}[-/]\d{2,4})/;
        const simpleMatch = body.match(simpleDateRegex);
        if (simpleMatch) data.date = simpleMatch[1];
    }

    // 5. Confidence Calculation
    if (data.amount !== null && data.type !== null && data.merchant !== "Unknown") {
        data.confidence = "high";
    }

    return data;
}

module.exports = { parseUPIMessage };
