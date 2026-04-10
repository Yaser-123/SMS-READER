/**
 * STRICT SMS PARSER ENGINE (v2.0)
 * Hardened for high-trust banking data.
 */

const bankKeywords = ["BOI", "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "CANARA", "PAYTM", "FEDERAL"];

/**
 * Stage 1: Primary Sender Filter
 */
function isValidBankSender(sender) {
    if (!sender) return false;
    const senderUpper = sender.toUpperCase();
    
    // Check for bank keywords
    const hasBankKeyword = bankKeywords.some(bank => senderUpper.includes(bank));
    
    // Strict Pattern: Exactly 2 letters + hyphen + Bank Alphanumeric
    // e.g. JD-BOIIND, VM-HDFCBK
    const matchesPattern = /^[A-Z]{2}-[A-Z0-9]{4,}/.test(senderUpper);
    
    return hasBankKeyword && matchesPattern;
}

/**
 * Stage 2: UPI Message Filter
 */
function isValidUPIMessage(body) {
    if (!body) return false;
    const bodyLower = body.toLowerCase();
    
    const hasUPITerm = body.toUpperCase().includes("UPI");
    const hasAction = /debited|credited|paid|received/.test(bodyLower);
        
    return hasUPITerm && hasAction;
}

/**
 * Stage 3: Strict Amount Extraction (Must be > 0)
 */
function extractAmount(body) {
    const amountRegex = /(Rs\.?|₹|INR)\s?([\d,]+(?:\.\d+)?)/i;
    const match = body.match(amountRegex);
    
    if (!match) return null;
    
    const rawValue = match[2].replace(/,/g, "");
    const amount = parseFloat(rawValue);
    
    // STRICT: Reject if 0, NaN, or negative
    if (!amount || isNaN(amount) || amount <= 0) return null;
    
    return amount;
}

/**
 * Stage 4: Advanced Reference Number Extraction
 */
function extractRefNo(body) {
    // Supports: Ref No.123, UPI Ref 123, RRN 123, vide No.123
    const refRegex = /(?:Ref(?:erence)?\s?No\.?|UPI Ref|RRN|No\.?|#)\s*[:.\s-]*\s*([a-z0-9]{6,})/i;
    const match = body.match(refRegex);
    
    if (!match) return null; // Discard if no ref number
    return match[1].trim();
}

/**
 * FINAL VALIDATION PIPELINE
 */
function parseStrictTransaction(sms) {
    const { body, sender, date } = sms;

    if (!isValidBankSender(sender)) return null;
    if (!isValidUPIMessage(body)) return null;

    const amount = extractAmount(body);
    if (amount === null) return null;

    const refNo = extractRefNo(body);
    if (refNo === null) return null;

    const bodyLower = body.toLowerCase();
    const type = (bodyLower.includes("debited") || bodyLower.includes("paid")) ? "debit" : "credit";

    // Refined Merchant Extraction
    const merchantRegex = /(?:to|from|at)\s+(.*?)(?:\s+via|\son|\sRef|\sis|$|\.)/i;
    const merchantMatch = body.match(merchantRegex);
    const merchant = merchantMatch ? merchantMatch[1].trim() : "Unknown";

    return {
        amount,
        type,
        merchant,
        reference_number: refNo,
        date: date || new Date().toISOString()
    };
}

module.exports = { parseStrictTransaction };
