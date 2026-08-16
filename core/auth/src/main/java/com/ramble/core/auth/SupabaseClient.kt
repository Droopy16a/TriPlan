package com.ramble.core.auth

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    internal const val SUPABASE_URL = "https://wkifuzmswxapltnitlgw.supabase.co"
    internal const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndraWZ1em1zd3hhcGx0bml0bGd3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MzQ1OTYsImV4cCI6MjEwMjMxMDU5Nn0.Krg0CqxFinXvrWDlVLflXuEVV_c2OYaxmTUg_n1bg8s"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
