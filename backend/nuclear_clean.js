/**
 * nuclear_clean.js — Run ONCE to clean the database
 * node backend/nuclear_clean.js
 */
const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = 'https://iqimfntocrsjgsfvrcbr.supabase.co';
const SUPABASE_KEY = 'sb_publishable_xj3-9XHFo4FvnI04Jx4vXQ_b0STQD6B';
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

async function nuclearClean() {
    console.log('🚨 Starting Nuclear Clean of duplicate/null reference rows...');

    // Step 1: Delete ALL rows where reference_number is literally 'null' or null
    const { data: deleted, error: e1 } = await supabase
        .from('transactions')
        .delete()
        .or('reference_number.is.null,reference_number.eq.null');

    if (e1) console.error('Step 1 Error:', e1.message);
    else console.log('✅ Step 1: Deleted null reference_number rows.');

    // Step 2: Delete rows with amount = 0 or null
    const { error: e2 } = await supabase
        .from('transactions')
        .delete()
        .or('amount.is.null,amount.eq.0');

    if (e2) console.error('Step 2 Error:', e2.message);
    else console.log('✅ Step 2: Deleted zero-amount rows.');

    // Step 3: Delete ALL score rows (force fresh recalculation)
    const { error: e3 } = await supabase
        .from('scores')
        .delete()
        .neq('id', '00000000-0000-0000-0000-000000000000');

    if (e3) console.error('Step 3 Error:', e3.message);
    else console.log('✅ Step 3: Cleared stale score history.');

    // Step 4: Verify what remains
    const { data: remaining } = await supabase
        .from('transactions')
        .select('id, reference_number, amount, type, merchant');

    console.log(`\n📊 Remaining transactions: ${remaining?.length ?? 0}`);
    
    // Show reference_number distribution
    const refCounts = {};
    remaining?.forEach(r => {
        refCounts[r.reference_number] = (refCounts[r.reference_number] || 0) + 1;
    });
    const dupes = Object.entries(refCounts).filter(([, v]) => v > 1);
    
    if (dupes.length > 0) {
        console.log('⚠️  Still has duplicates:', dupes);
    } else {
        console.log('✅ All reference_numbers are unique. Database is clean.');
        console.log(`\n🎯 Your clean transaction count: ${remaining?.length ?? 0}`);
        console.log('   Now restart npm start and sync once. Your score will be stable.');
    }
}

nuclearClean().catch(console.error);
