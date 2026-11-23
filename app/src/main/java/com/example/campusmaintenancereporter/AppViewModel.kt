package com.example.campusmaintenancereporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val store: ReportStore = InMemoryReportStore(),
    private val auth: AuthService = DemoAuthService()
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    /**
     * Reporters see only their own reports, technicians only the jobs assigned to them,
     * and supervisors see everything. The database applies the same rules on the server.
     */
    val visibleReports: StateFlow<List<Report>> =
        combine(store.reports, _session) { all, s ->
            when {
                s == null -> emptyList()
                s.role == Role.SUPERVISOR -> all
                s.role == Role.MAINTENANCE -> all.filter { it.assignedTo == s.name }
                else -> all.filter { it.reporterEmail == s.email }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun authenticate(email: String, password: String): AuthResult = auth.authenticate(email, password)

    fun signIn(email: String, role: Role, name: String = DemoAccounts.nameOf(email)) {
        val s = Session(email, role, name)
        _session.value = s
        store.start(s)
    }

    fun signOut() {
        store.stop()
        auth.signOut()
        _session.value = null
    }

    /** Sends a new report. onDone gets the reference number, or null if it could not be sent. */
    fun submitReport(newReport: NewReport, onDone: (String?) -> Unit) {
        val s = _session.value
        if (s == null) {
            onDone(null)
            return
        }
        viewModelScope.launch {
            val reference = try {
                store.add(newReport, s.email, s.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onDone(reference)
        }
    }

    fun updateStatus(id: String, status: Status, text: String) {
        val s = _session.value ?: return
        val current = store.reports.value.firstOrNull { it.id == id } ?: return

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

        launchSafely { store.change(id = id, author = s.name, text = text, newStatus = status) }
    }

    fun addNote(id: String, text: String) {
        val s = _session.value ?: return
        val current = store.reports.value.firstOrNull { it.id == id } ?: return
        if (!canWork(s, current)) return
        launchSafely { store.change(id = id, author = s.name, text = text) }
    }

    fun assign(id: String, technician: String) {
        val s = _session.value ?: return
        if (s.role != Role.SUPERVISOR) return
        launchSafely { store.change(id = id, author = s.name, text = "Assigned to $technician", assignTo = technician) }
    }

    fun escalate(id: String) {
        val s = _session.value ?: return
        val report = store.reports.value.firstOrNull { it.id == id } ?: return
        if (s.role.isMaintenance || report.reporterEmail != s.email) return
        if (report.escalated || System.currentTimeMillis() < report.escalationOpensAt) return
        launchSafely { store.change(id = id, author = s.name, text = "Escalated: still not fixed", escalate = true) }
    }

    suspend fun loadPhoto(id: String): String? = try {
        store.loadPhoto(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** Loads the sample reports into an empty database (supervisors only). */
    fun loadSamples(onDone: (Boolean) -> Unit) {
        if (_session.value?.role != Role.SUPERVISOR) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val ok = try {
                store.seedSamples()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onDone(ok)
        }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The server refused the change, so the list stays as the server has it
            }
        }
    }

    /** Supervisors can work on any job. Technicians only on jobs assigned to them. */
    private fun canWork(s: Session, r: Report): Boolean = when (s.role) {
        Role.SUPERVISOR -> true
        Role.MAINTENANCE -> r.assignedTo == s.name
        else -> false
    }
}