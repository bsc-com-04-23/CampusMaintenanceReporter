package com.example.campusmaintenancereporter


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppViewModel : ViewModel() {
    private val repo = ReportRepository()

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    /** Reporters see only their own reports; maintenance and supervisors see everything. */
    val visibleReports: StateFlow<List<Report>> =
        combine(repo.reports, _session) { all, s ->
            when {
                s == null -> emptyList()
                s.role.isMaintenance -> all
                else -> all.filter { it.reporterEmail == s.email }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun signIn(email: String, role: Role) { _session.value = Session(email, role) }
    fun signOut() { _session.value = null }

    fun submitReport(newReport: NewReport): String =
        repo.add(newReport, _session.value?.email ?: "")

    fun updateStatus(id: String, status: Status, text: String) {
        val s = _session.value ?: return
        repo.change(
            id = id,
            author = authorOf(s),
            text = text,
            newStatus = status,
            assignTo = if (s.role.isMaintenance) displayName(s) else null,
            clearOverdue = s.role.isMaintenance
        )
    }

    fun addNote(id: String, text: String) {
        val s = _session.value ?: return
        repo.change(id, authorOf(s), text)
    }

    fun escalate(id: String) {
        val s = _session.value ?: return
        repo.change(id, authorOf(s), "Escalated: still not fixed")
    }

    private fun displayName(s: Session): String =
        s.email.substringBefore("@").replaceFirstChar { it.uppercase() }

    private fun authorOf(s: Session): String =
        if (s.role.isMaintenance) displayName(s) else "You"
}