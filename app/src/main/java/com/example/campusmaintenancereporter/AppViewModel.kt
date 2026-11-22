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

    /**
     * Reporters see only their own reports, technicians only the jobs assigned to them,
     * and supervisors see everything.
     */
    val visibleReports: StateFlow<List<Report>> =
        combine(repo.reports, _session) { all, s ->
            when {
                s == null -> emptyList()
                s.role == Role.SUPERVISOR -> all
                s.role == Role.MAINTENANCE -> all.filter { it.assignedTo == nameOf(s) }
                else -> all.filter { it.reporterEmail == s.email }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun signIn(email: String, role: Role) { _session.value = Session(email, role) }
    fun signOut() { _session.value = null }

    fun submitReport(newReport: NewReport): String {
        val s = _session.value
        return repo.add(newReport, s?.email ?: "", if (s != null) nameOf(s) else "")
    }

    fun updateStatus(id: String, status: Status, text: String) {
        val s = _session.value ?: return
        val current = repo.reports.value.firstOrNull { it.id == id } ?: return

        val allowed = if (s.role.isMaintenance) {
            // Technicians and supervisors do the work: In Progress or Resolved only
            current.isOpen && canWork(s, current) &&
                    (status == Status.IN_PROGRESS || status == Status.RESOLVED)
        } else {
            // Reporters only confirm or reopen their own resolved report
            current.reporterEmail == s.email &&
                    current.status == Status.RESOLVED &&
                    (status == Status.CLOSED || status == Status.REOPENED)
        }
        if (!allowed) return

        repo.change(id = id, author = nameOf(s), text = text, newStatus = status)
    }

    fun addNote(id: String, text: String) {
        val s = _session.value ?: return
        val current = repo.reports.value.firstOrNull { it.id == id } ?: return
        if (!canWork(s, current)) return
        repo.change(id, nameOf(s), text)
    }

    fun assign(id: String, technician: String) {
        val s = _session.value ?: return
        if (s.role != Role.SUPERVISOR) return
        repo.change(id, nameOf(s), "Assigned to $technician", assignTo = technician)
    }

    fun escalate(id: String) {
        val s = _session.value ?: return
        val report = repo.reports.value.firstOrNull { it.id == id } ?: return
        if (s.role.isMaintenance || report.reporterEmail != s.email) return
        if (report.escalated || System.currentTimeMillis() < report.escalationOpensAt) return
        repo.change(id, nameOf(s), "Escalated: still not fixed", escalate = true)
    }

    /** Supervisors can work on any job. Technicians only on jobs assigned to them. */
    private fun canWork(s: Session, r: Report): Boolean = when (s.role) {
        Role.SUPERVISOR -> true
        Role.MAINTENANCE -> r.assignedTo == nameOf(s)
        else -> false
    }

    private fun nameOf(s: Session): String = DemoAccounts.nameOf(s.email)
}