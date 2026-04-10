# REQUIRED: Run this SQL once in your Supabase SQL Editor
# Go to: https://supabase.com/dashboard/project/iqimfntocrsjgsfvrcbr/sql

Run the following SQL in the Supabase SQL Editor to enforce the unique constraint
that makes the upsert idempotent (this is why your score keeps jumping):

```sql
-- Step 1: Clean any remaining null/duplicate reference_numbers
DELETE FROM transactions
WHERE reference_number IS NULL
   OR reference_number = 'null'
   OR amount IS NULL
   OR amount = 0;

-- Step 2: Remove duplicate reference_numbers (keep the first/oldest one)
DELETE FROM transactions
WHERE id NOT IN (
    SELECT MIN(id)
    FROM transactions
    GROUP BY reference_number
);

-- Step 3: Add the unique constraint so upsert works correctly
ALTER TABLE transactions
ADD CONSTRAINT unique_reference_number UNIQUE (reference_number);

-- Step 4: Verify the constraint was added
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'transactions';
```

After running this SQL:
1. Restart your backend server (Ctrl+C then `npm start`)
2. Sync your phone once
3. Your score will be stable and deterministic
