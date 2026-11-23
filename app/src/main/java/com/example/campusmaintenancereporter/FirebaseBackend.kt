package com.example.campusmaintenancereporter

import android.util.Base64
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "CampusFirebase"

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthService {

    override suspend fun authenticate(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val doc = db.collection("users").document(email).get().await()
            val role = doc.getString("role")?.let { runCatching { Role.valueOf(it) }.getOrNull() }
            if (role == null) {
                auth.signOut()
                AuthResult.Failure("This account has no role yet. Please ask the administrator.")
            } else {
                AuthResult.Success(Session(email, role, doc.getString("name") ?: email.substringBefore("@")))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            AuthResult.Failure("No internet connection. Please check it and try again.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Failure("Incorrect email or password.")
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Failure("Incorrect email or password.")
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            AuthResult.Failure("Could not sign in. Please try again.")
        }
    }

    override fun signOut() = auth.signOut()
}

class FirebaseReportStore(
    private val cacheDir: File,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReportStore {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    override val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    private var registration: ListenerRegistration? = null

    override fun start(session: Session) {
        stop()
        val base = db.collection("reports")
        // The query has to match what the security rules let this user read
        val query: Query = when (session.role) {
            Role.SUPERVISOR -> base
            Role.MAINTENANCE -> base.whereEqualTo("assignedTo", session.name)
            else -> base.whereEqualTo("reporterEmail", session.email)
        }
        registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Could not load reports", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                _reports.value = snapshot.documents.mapNotNull { it.toReport() }.sortedByDescending { it.createdAt }
            }
        }
    }

    override fun stop() {
        registration?.remove()
        registration = null
        _reports.value = emptyList()
    }

    override suspend fun add(newReport: NewReport, reporterEmail: String, reporterName: String): String {
        // The next report number comes from a counter, so two phones never get the same one
        val counter = db.collection("meta").document("counter")
        val reference = db.runTransaction(Transaction.Function { tx ->
            val next = tx.get(counter).getLong("next") ?: 1048L
            tx.set(counter, mapOf("next" to next + 1))
            "CMR-$next"
        }).await()

        val created = System.currentTimeMillis()
        val photoData = newReport.photoPath?.let { PhotoUtils.encodeForUpload(it) }
        val report = Report(
            id = reference,
            title = newReport.title,
            category = newReport.category,
            location = newReport.location,
            description = newReport.description,
            priority = newReport.priority,
            status = Status.REPORTED,
            createdAt = created,
            timeline = listOf(TimelineEntry(formatDate(created, "d MMM, HH:mm"), reporterName, "Report submitted")),
            reporterEmail = reporterEmail,
            hasPhoto = photoData != null
        )

        val batch = db.batch()
        batch.set(db.collection("reports").document(reference), report.toMap())
        if (photoData != null) {
            batch.set(db.collection("photos").document(reference), mapOf("data" to photoData))
        }
        batch.commit().await()
        return reference
    }

    override suspend fun change(
        id: String, author: String, text: String,
        newStatus: Status?, assignTo: String?, escalate: Boolean
    ) {
        val entry = mapOf(
            "time" to formatDate(System.currentTimeMillis(), "d MMM, HH:mm"),
            "author" to author,
            "text" to text
        )
        val updates = mutableMapOf<String, Any>("timeline" to FieldValue.arrayUnion(entry))
        if (newStatus != null) updates["status"] = newStatus.name
        if (assignTo != null) updates["assignedTo"] = assignTo
        if (escalate) updates["escalated"] = true
        db.collection("reports").document(id).update(updates).await()
    }

    /** Downloads the photo once and keeps a copy on the phone. */
    override suspend fun loadPhoto(id: String): String? = withContext(Dispatchers.IO) {
        val file = File(File(cacheDir, "cloud_photos").apply { mkdirs() }, "$id.jpg")
        if (file.exists() && file.length() > 0) return@withContext file.absolutePath
        val data = db.collection("photos").document(id).get().await().getString("data")
            ?: return@withContext null
        file.writeBytes(Base64.decode(data, Base64.NO_WRAP))
        file.absolutePath
    }

    /** Loads the sample reports from Models.kt into the database (supervisors only). */
    override suspend fun seedSamples(): Boolean = try {
        val batch = db.batch()
        SampleData.reports.forEach { r ->
            batch.set(db.collection("reports").document(r.id), r.toMap(seed = true))
        }
        batch.commit().await()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Could not load the sample reports", e)
        false
    }
}

private fun Report.toMap(seed: Boolean = false): Map<String, Any?> = mapOf(
    "title" to title,
    "category" to category,
    "location" to location,
    "description" to description,
    "priority" to priority.name,
    "status" to status.name,
    "createdAt" to createdAt,
    "dueAt" to dueAt,
    "assignedTo" to assignedTo,
    "reporterEmail" to reporterEmail,
    "escalated" to escalated,
    "hasPhoto" to hasPhoto,
    "seed" to seed,
    "timeline" to timeline.map { mapOf("time" to it.time, "author" to it.author, "text" to it.text) }
)

private fun DocumentSnapshot.toReport(): Report? = try {
    val entries = (get("timeline") as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let {
            TimelineEntry(it["time"] as? String ?: "", it["author"] as? String ?: "", it["text"] as? String ?: "")
        }
    } ?: emptyList()
    val priority = Priority.valueOf(getString("priority") ?: "MEDIUM")
    val created = getLong("createdAt") ?: 0L
    Report(
        id = id,
        title = getString("title") ?: "",
        category = getString("category") ?: "",
        location = getString("location") ?: "",
        description = getString("description") ?: "",
        priority = priority,
        status = Status.valueOf(getString("status") ?: "REPORTED"),
        createdAt = created,
        assignedTo = getString("assignedTo"),
        timeline = entries,
        reporterEmail = getString("reporterEmail") ?: "",
        dueAt = getLong("dueAt") ?: (created + priority.targetDays * DAY_MS),
        escalated = getBoolean("escalated") ?: false,
        hasPhoto = getBoolean("hasPhoto") ?: false
    )
} catch (e: Exception) {
    null
}