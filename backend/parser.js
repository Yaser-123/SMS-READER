const crypto = require('crypto');

/**
 * SMS Parser utility
 * Extracts Amount, Merchant, Type, Date, and Reference Number from UPI messages.
 */

function parseUPIMessage(body) {
    if (!body) return null;

    const data = {
        amount: null,
        type: null,
        merchant: "Unknown",
        date: "Unknown",
        referenceNumber: null,
        confidence: "low"
    };

    // 1. Normalize and Extract Amount
    const amountRegex = /(Rs\.?|₹|INR)\s?([\d,]+(?:\.\d+)?)/i;
    const amountMatch = body.match(amountRegex);
    
    if (amountMatch) {
        const rawAmount = amountMatch[2].replace(/,/g, '');
        const amount = parseFloat(rawAmount);

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

    // 3. Extract Reference Number (Improved for broader formats)
    // Supports: Ref:123, Ref.123, No.123, vide No.123, UPI Ref:123, RRN:123
    const refRegex = /(?:Ref\.?|UPI Ref|RRN|Trans ID|No\.?|#|vide\s+No\.?|ID|RefNo)\s*[:.\s-]*\s*([a-z0-9]+)/i;
    const refMatch = body.match(refRegex);
    
    if (refMatch) {
        data.referenceNumber = refMatch[1].trim();
    } else {
        // Fallback: Deterministic Hash (Body + Month/Year if found)
        data.referenceNumber = crypto.createHash('md5').update(body).digest('hex').substring(0, 12);
    }

    // 4. Extract Merchant Name
    const merchantRegex = /(?:to|from|at)\s+(.*?)(?:\s+via|\son|\sRef|$|\.|\sis)/i;
    const merchantMatch = body.match(merchantRegex);
    if (merchantMatch) {
        data.merchant = merchantMatch[1].trim();
    }

    // 5. Extract Date
    const dateRegex = /(?:on\s)(\d{2}[a-zA-Z]{3}\d{2})/i;
    const dateMatch = body.match(dateRegex);
    if (dateMatch) {
        data.date = dateMatch[1];
    } else {
        const simpleDateRegex = /(\d{2}[-/]\d{2}[-/]\d{2,4})/;
        const simpleMatch = body.match(simpleDateRegex);
        if (simpleMatch) data.date = simpleMatch[1];
    }

    // 6. Confidence Calculation
    if (data.amount !== null && data.type !== null && data.merchant !== "Unknown") {
        data.confidence = "high";
    }

    return data;
}

module.exports = { parseUPIMessage };
