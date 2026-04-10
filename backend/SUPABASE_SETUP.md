# REQUIRED: Run this SQL once in your Supabase SQL Editor
# Go to: https://supabase.com/dashboard/project/iqimfntocrsjgsfvrcbr/sql

Run the following SQL in the Supabase SQL Editor to enforce the unique constraint
that makes the upsert idempotent (this is why your score keeps jumping):

```sql
-- Step 1: Clean null/zero rows
DELETE FROM transactions
WHERE reference_number IS NULL
   OR reference_number = 'null'
   OR amount IS NULL
   OR amount = 0;

-- Step 2: Remove duplicates — keep the EARLIEST row per reference_number
-- (Uses created_at since id is a UUID and MIN(uuid) is not supported)
DELETE FROM transactions t1
USING transactions t2
WHERE t1.reference_number = t2.reference_number
  AND t1.created_at > t2.created_at;

-- Step 3: Add unique constraint so upsert works forever
ALTER TABLE transactions
ADD CONSTRAINT unique_reference_number UNIQUE (reference_number);

-- Step 4: Verify
SELECT constraint_name FROM information_schema.table_constraints
WHERE table_name = 'transactions' AND constraint_type = 'UNIQUE';
```

After running this SQL:
1. Restart your backend server (Ctrl+C then `npm start`)
2. Sync your phone once
3. Your score will be stable and deterministic
