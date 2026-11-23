package com.example.campusmaintenancereporter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow

/** Switches the whole app to the built-in demo data. Only the full-app tests turn this on. */
object AppConfig {
    var useDemoBackend = false
}

sealed interface AuthResult {
    data class Success(val session: Session) : AuthResult
    data class Failure(val message: String) : AuthResult
}

interface AuthService {
    suspend fun authenticate(email: String, password: String): AuthResult
    fun signOut() {}
}

/** The demo accounts from Models.kt. Used by the tests. */
class DemoAuthService : AuthService {
    override suspend fun authenticate(email: String, password: String): AuthResult {
        val clean = email.trim().lowercase()
        val role = DemoAccounts.login(clean, password)
        return if (role != null) {
            AuthResult.Success(Session(clean, role, DemoAccounts.nameOf(clean)))
        } else {
            AuthResult.Failure("Incorrect email or password.")
        }
    }
}

interface ReportStore {
    val reports: StateFlow<List<Report>>
    fun start(session: Session) {}
    fun stop() {}
    suspend fun add(newReport: NewReport, reporterEmail: String, reporterName: String): String
    suspend fun change(
        id: String,
        author: String,
        text: String,
        newStatus: Status? = null,
        assignTo: String? = null,
        escalate: Boolean = false
    )
    suspend fun loadPhoto(id: String): String? = null
    suspend fun seedSamples(): Boolean = false
}

/** The in-memory reports from ReportRepository. Used by the tests. */
class InMemoryReportStore(private val repo: ReportRepository = ReportRepository()) : ReportStore {
    override val reports: StateFlow<List<Report>> = repo.reports

    override suspend fun add(newReport: NewReport, reporterEmail: String, reporterName: String): String =
        repo.add(newReport, reporterEmail, reporterName)

    override suspend fun change(
        id: String, author: String, text: String,
        newStatus: Status?, assignTo: String?, escalate: Boolean
    ) = repo.change(id, author, text, newStatus, assignTo, escalate)
}

class AppViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm = if (AppConfig.useDemoBackend) {
            AppViewModel()
        } else {
            AppViewModel(FirebaseReportStore(appContext.cacheDir), FirebaseAuthService())
        }
        return vm as T
    }
}