-- Destructive reset for Ramble trip sharing.
-- Paste this into the Supabase SQL Editor only if you are OK deleting existing app data.

drop table if exists public.trip_members cascade;
drop table if exists public.trips cascade;
drop table if exists public.profiles cascade;

drop trigger if exists on_auth_user_created on auth.users;
drop function if exists public.handle_new_user();
drop function if exists public.is_trip_member(uuid, uuid);
drop function if exists public.is_trip_owner(uuid, uuid);
drop function if exists public.join_trip(uuid);
drop function if exists public.touch_updated_at();

create extension if not exists pgcrypto;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  first_name text not null default '',
  last_name text not null default '',
  email text not null default '',
  birth_date text not null default '',
  phone_country_code text not null default '',
  phone_number text not null default '',
  travel_style text not null default 'Balanced',
  interests jsonb not null default '[]'::jsonb,
  accommodation_preference jsonb not null default '[]'::jsonb,
  transportation_preference jsonb not null default '[]'::jsonb,
  food_preferences jsonb not null default '[]'::jsonb,
  notifications_enabled boolean not null default true,
  notifications jsonb not null default '[]'::jsonb,
  language text not null default 'English',
  currency text not null default 'EUR (€)',
  units text not null default 'Metric (km)',
  theme text not null default 'Light',
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.trips (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  destination text not null,
  dates text not null default '',
  travelers text not null default '',
  budget text not null default '',
  preferences text not null default '',
  emoji text not null default '✈️',
  image_url text,
  itinerary jsonb,
  expenses jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.trip_members (
  trip_id uuid not null references public.trips(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (trip_id, user_id)
);

create index trip_members_user_id_idx on public.trip_members(user_id);
create index trip_members_trip_id_idx on public.trip_members(trip_id);
create index trips_user_id_idx on public.trips(user_id);

create function public.is_trip_member(p_trip_id uuid, p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.trip_members tm
    where tm.trip_id = p_trip_id
      and tm.user_id = p_user_id
  );
$$;

create function public.is_trip_owner(p_trip_id uuid, p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.trips t
    where t.id = p_trip_id
      and t.user_id = p_user_id
  );
$$;

create function public.join_trip(p_trip_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;

  if not exists (select 1 from public.trips where id = p_trip_id) then
    raise exception 'Trip not found';
  end if;

  insert into public.trip_members (trip_id, user_id)
  values (p_trip_id, auth.uid())
  on conflict (trip_id, user_id) do nothing;
end;
$$;

create function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_touch_updated_at
before update on public.profiles
for each row execute function public.touch_updated_at();

create trigger trips_touch_updated_at
before update on public.trips
for each row execute function public.touch_updated_at();

alter table public.profiles enable row level security;
alter table public.trips enable row level security;
alter table public.trip_members enable row level security;

create policy "profiles_select_authenticated"
on public.profiles
for select
to authenticated
using (true);

create policy "profiles_insert_own"
on public.profiles
for insert
to authenticated
with check (id = auth.uid());

create policy "profiles_update_own"
on public.profiles
for update
to authenticated
using (id = auth.uid())
with check (id = auth.uid());

create policy "trips_select_owner_or_member"
on public.trips
for select
to authenticated
using (
  user_id = auth.uid()
  or public.is_trip_member(id, auth.uid())
);

create policy "trips_insert_own"
on public.trips
for insert
to authenticated
with check (user_id = auth.uid());

create policy "trips_update_owner_or_member"
on public.trips
for update
to authenticated
using (
  user_id = auth.uid()
  or public.is_trip_member(id, auth.uid())
)
with check (
  user_id = auth.uid()
  or public.is_trip_member(id, auth.uid())
);

create policy "trips_delete_owner"
on public.trips
for delete
to authenticated
using (user_id = auth.uid());

create policy "trip_members_select_same_trip"
on public.trip_members
for select
to authenticated
using (public.is_trip_member(trip_id, auth.uid()));

create policy "trip_members_insert_self"
on public.trip_members
for insert
to authenticated
with check (user_id = auth.uid());

create policy "trip_members_update_self"
on public.trip_members
for update
to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

create policy "trip_members_delete_self_or_owner"
on public.trip_members
for delete
to authenticated
using (
  user_id = auth.uid()
  or public.is_trip_owner(trip_id, auth.uid())
);

grant execute on function public.join_trip(uuid) to authenticated;

-- Auto-create a profile row whenever a new auth user is created (email or OAuth).
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
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
