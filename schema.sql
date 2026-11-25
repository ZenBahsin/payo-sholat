-- Payo Shalat: Relational PostgreSQL / Supabase Reference Schema (with RLS)
-- Note: The native Android app uses Firebase Auth + Cloud Firestore directly
-- (see firebase-blueprint.json & firestore.rules). This SQL schema provides an
-- equivalent PostgreSQL/Supabase migration reference if you deploy a companion web backend.

create type prayer_name_enum as enum ('subuh', 'zuhur', 'asar', 'maghrib', 'isya');

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  avatar_url text,
  city text not null default 'Kota Palembang',
  region text default 'Sumatera Selatan',
  country_code text not null default 'ID' check (country_code in ('ID', 'MY')),
  jakim_zone text,
  timezone_id text not null default 'Asia/Jakarta',
  latitude double precision not null default -2.9909,
  longitude double precision not null default 104.7567,
  notification_prefs jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.prayer_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  prayer_date date not null,
  prayer_name prayer_name_enum not null,
  is_completed boolean not null default true,
  pray_mode text not null default 'sendiri' check (pray_mode in ('sendiri', 'berjamaah_masjid')),
  completed_at timestamptz not null default now(),
  unique (user_id, prayer_date, prayer_name)
);

create table if not exists public.quran_progress (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  last_surah_number integer not null default 0 check (last_surah_number between 0 and 114),
  last_surah_name text not null default '',
  last_ayah integer not null default 0 check (last_ayah >= 0),
  total_ayahs_read integer not null default 0 check (total_ayahs_read between 0 and 6236),
  khatam_percentage numeric(5,2) not null default 0 check (khatam_percentage between 0 and 100),
  bookmarks jsonb not null default '[]'::jsonb,
  updated_at timestamptz not null default now()
);

create table if not exists public.tasbih_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  dzikir_title text not null default 'Subhanallah',
  current_count integer not null default 0 check (current_count >= 0),
  target_count integer not null default 100 check (target_count >= 1),
  updated_at timestamptz not null default now()
);

-- Enable Row Level Security (RLS) on all tables
alter table public.profiles enable row level security;
alter table public.prayer_logs enable row level security;
alter table public.quran_progress enable row level security;
alter table public.tasbih_sessions enable row level security;

-- Strictly isolate each user's data
create policy "profiles_owner_all" on public.profiles
  for all using (auth.uid() = id) with check (auth.uid() = id);

create policy "prayer_logs_owner_all" on public.prayer_logs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "quran_progress_owner_all" on public.quran_progress
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "tasbih_sessions_owner_all" on public.tasbih_sessions
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
