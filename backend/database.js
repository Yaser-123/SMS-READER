const { createClient } = require('@supabase/supabase-js');

// Config
const SUPABASE_URL = 'https://iqimfntocrsjgsfvrcbr.supabase.co';
const SUPABASE_KEY = 'sb_publishable_xj3-9XHFo4FvnI04Jx4vXQ_b0STQD6B';

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

/**
 * SMART PRUNE: Deletes only NULL or 0 amount rows (Junk)
 */
async function pruneJunk() {
    console.log("🧹 Pruning misidentified junk records...");
    const { error } = await supabase.from('transactions').delete().or('amount.is.null,amount.eq.0');
    if (error) console.error("⚠️ Prune Warning:", error.message);
}

/**
 * NUCLEAR RESET: Wipes everything
 */
async function clearAllData() {
    console.log("🚀 Initializing Nuclear Clean-Slate...");
    try {
        await supabase.from('scores').delete().neq('id', '00000000-0000-0000-0000-000000000000');
        await supabase.from('transactions').delete().neq('id', '00000000-0000-0000-0000-000000000000');
        console.log("✅ Cleanup Complete. System Reset.");
    } catch (e) {
        console.error("❌ Cleanup Error:", e.message);
    }
}

/**
 * Saves a batch of parsed transactions to Supabase.
 * Uses UPSERT with 'reference_number' conflict resolution for idempotency.
 * SQL: ALTER TABLE transactions ADD CONSTRAINT unique_ref UNIQUE (reference_number);
 */
async function saveTransactions(transactions) {
    if (transactions.length === 0) return;

    // --- STEP 1: PRUNE JUNK (Auto-Clean) ---
    // Remove any existing rows with NULL or 0 amount to clear old garbage
    await supabase
        .from('transactions')
        .delete()
        .or('amount.is.null,amount.eq.0');

    // --- STEP 2: CODE-LEVEL DE-DUPLICATION ---
    // Fetch last 100 reference numbers to prevent same-sync or recent duplicates
    const { data: existingRefs } = await supabase
        .from('transactions')
        .select('reference_number')
        .order('date', { ascending: false })
        .limit(100);

    const existingSet = new Set(existingRefs?.map(r => r.reference_number) || []);
    
    const finalRows = transactions
        .filter(tx => !existingSet.has(tx.reference_number))
        .map(item => ({
            amount: item.amount,
            type: item.type,
            merchant: item.merchant,
            reference_number: item.reference_number,
            date: item.date,
            raw_message: item.raw_message
        }));

    if (finalRows.length === 0) return;

    // --- STEP 3: PERSIST ---
    // Use UPSERT on reference_number to handle bank retries vs duplicates
    const { error } = await supabase
        .from('transactions')
        .upsert(finalRows, { onConflict: 'reference_number', ignoreDuplicates: true });

    if (error) console.error('Error saving transactions (UPSERT ref):', error.message);
}

    if (error) console.error('Error saving transactions (UPSERT ref):', error.message);
}

    if (error) console.error('Error saving transactions (UPSERT ref):', error.message);
}

/**
 * Saves a calculated score and breakdown to Supabase.
 */
async function saveScore(scoreData) {
    const { error } = await supabase
        .from('scores')
        .insert([{
            score: scoreData.score,
            risk: scoreData.risk,
            total_credit: scoreData.features.totalCredit,
            total_debit: scoreData.features.totalDebit,
            transaction_count: scoreData.features.transactionCount,
            breakdown: scoreData.breakdown 
        }]);

    if (error) console.error('Error saving score:', error.message);
}

/**
 * Fetches transaction history and latest score entries.
 */
async function getHistory() {
    const { data: transactions, error: tError } = await supabase
        .from('transactions')
        .select('*')
        .order('date', { ascending: false })
        .limit(50);

    const { data: scores, error: sError } = await supabase
        .from('scores')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(2); 

    if (tError || sError) {
        console.error('Error fetching history:', tError?.message || sError?.message);
        return { transactions: [], latestScores: [] };
    }

    return {
        transactions: transactions || [],
        latestScores: scores || []
    };
}

module.exports = {
    saveTransactions,
    saveScore,
    getHistory,
    clearAllData,
    pruneJunk
};
