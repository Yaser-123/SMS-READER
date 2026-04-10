/**
 * ensure_unique_constraint.js
 *
 * Run this ONCE to guarantee the DB constraint exists:
 *   node backend/ensure_unique_constraint.js
 *
 * Without this constraint, upsert silently falls back to INSERT
 * and creates duplicate rows — causing the score to jump.
 */
const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = 'https://iqimfntocrsjgsfvrcbr.supabase.co';
const SUPABASE_KEY = 'sb_publishable_xj3-9XHFo4FvnI04Jx4vXQ_b0STQD6B';
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

async function ensureConstraint() {
    console.log('Checking for duplicate reference_numbers in transactions table...');

    // 1. Show duplicate reference_numbers
    const { data: dupes, error: dupeError } = await supabase.rpc('exec_sql', {
        query: `
            SELECT reference_number, COUNT(*) as cnt
            FROM transactions
            GROUP BY reference_number
            HAVING COUNT(*) > 1
            ORDER BY cnt DESC;
        `
    });

    if (dupeError) {
        console.log('Cannot call exec_sql directly. Checking via select instead...');

        const { data: all, error } = await supabase
            .from('transactions')
            .select('reference_number');

        if (error) {
            console.error('Error:', error.message);
            return;
        }

        const counts = {};
        all.forEach(r => { counts[r.reference_number] = (counts[r.reference_number] || 0) + 1; });
        const duplicates = Object.entries(counts).filter(([, v]) => v > 1);

        if (duplicates.length > 0) {
            console.log(`\n⚠️  FOUND ${duplicates.length} DUPLICATE reference_numbers!`);
            console.log('This is WHY your score jumps. Duplicates:\n', duplicates.slice(0, 10));
            console.log('\n⚠️  ACTION REQUIRED: Go to Supabase Dashboard → Table Editor → transactions');
            console.log('   Run this SQL in the SQL Editor:\n');
            console.log('   -- Step 1: Delete duplicates (keep oldest)');
            console.log('   DELETE FROM transactions');
            console.log('   WHERE id NOT IN (');
            console.log('       SELECT MIN(id) FROM transactions GROUP BY reference_number');
            console.log('   );');
            console.log('\n   -- Step 2: Add unique constraint');
            console.log('   ALTER TABLE transactions');
            console.log('   ADD CONSTRAINT IF NOT EXISTS unique_reference_number UNIQUE (reference_number);');
        } else {
            console.log(`\n✅ No duplicates found. Total rows: ${all.length}`);
            console.log('   The unique constraint should be working. Score issue is in the merge logic.');
        }
        return;
    }

    console.log('Duplicates found:', dupes);
}

ensureConstraint().catch(console.error);
