-- Fix for: null value in column "birth_date" of relation "profiles" violates not-null constraint
-- Root cause: a stale handle_new_user trigger explicitly listed birth_date without a fallback.
--
-- Run this in the Supabase SQL Editor (safe, non-destructive — does NOT drop existing tables).

-- 1. Drop the broken trigger and its function
drop trigger if exists on_auth_user_created on auth.users;
drop function if exists public.handle_new_user();

-- 2. Recreate with a correct implementation that reads from raw_user_meta_data
--    and safely defaults every optional field.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (
    id,
    first_name,
    last_name,
    email,
    avatar_url
  )
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'first_name', ''),
    coalesce(new.raw_user_meta_data->>'last_name',  ''),
    coalesce(new.email, ''),
    new.raw_user_meta_data->>'avatar_url'
  )
  on conflict (id) do nothing;   -- idempotent: safe if profile already exists
  return new;
end;
$$;

-- 3. Attach the trigger to auth.users
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
