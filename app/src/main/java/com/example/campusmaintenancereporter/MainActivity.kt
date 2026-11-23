package com.example.campusmaintenancereporter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaintenanceTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav(
    vm: AppViewModel = viewModel(factory = AppViewModelFactory(LocalContext.current.applicationContext))
) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val session by vm.session.collectAsState()
    val reports by vm.visibleReports.collectAsState()
    val role = session?.role ?: Role.STUDENT
    val userName = session?.name ?: ""

    val logout: () -> Unit = {
        vm.signOut()
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }
    val openDetails: (String) -> Unit = { id -> nav.navigate("details/$id") }

    // Switch between tabs without stacking screens
    fun goTab(route: String, root: String) {
        nav.navigate(route) {
            popUpTo(root) { inclusive = false }
            launchSingleTop = true
        }
    }

    val reporterBar: @Composable (Int) -> Unit = { selected ->
        AppBottomBar(
            tabs = listOf(
                NavTab("Home", Icons.Default.Home) { goTab("home", "home") },
                NavTab("Report", Icons.Default.Add) { nav.navigate("report") },
                NavTab("My Reports", Icons.AutoMirrored.Filled.List) { goTab("myreports", "home") }
            ),
            selected = selected
        )
    }

    // Supervisors have two tabs. Technicians have a single list of their own jobs.
    val supervisorBar: @Composable (Int) -> Unit = { selected ->
        AppBottomBar(
            tabs = listOf(
                NavTab("All Jobs", Icons.AutoMirrored.Filled.List) { goTab("dashboard", "dashboard") },
                NavTab("Unassigned", Icons.Default.Warning) { goTab("unassigned", "dashboard") }
            ),
            selected = selected
        )
    }

    // The sample-data button only appears for a supervisor, on an empty database
    val seed: (() -> Unit)? = if (role == Role.SUPERVISOR) {
        {
            vm.loadSamples { ok ->
                if (!ok) Toast.makeText(context, "Could not load the sample reports.", Toast.LENGTH_SHORT).show()
            }
        }
    } else null

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                authenticate = { email, password -> vm.authenticate(email, password) },
                onLogin = { s ->
                    vm.signIn(s.email, s.role, s.name)
                    nav.navigate(if (s.role.isMaintenance) "dashboard" else "home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                role = role,
                userName = userName,
                reports = reports,
                onReport = { nav.navigate("report") },
                onMyReports = { nav.navigate("myreports") },
                onStatusClick = { s -> nav.navigate("myreports?status=$s") },
                onOpen = openDetails,
                onLogout = logout,
                bottomBar = { reporterBar(0) }
            )
        }
        composable("report") {
            ReportFormScreen(
                onBack = { nav.popBackStack() },
                onSubmit = { newReport, done -> vm.submitReport(newReport, done) },
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            route = "myreports?status={status}",
            arguments = listOf(navArgument("status") {
                type = NavType.StringType
                defaultValue = "ALL"
            })
        ) { entry ->
            val wanted = entry.arguments?.getString("status")
            MyReportsScreen(
                reports = reports,
                onBack = { nav.popBackStack() },
                onOpen = openDetails,
                initialFilter = Status.entries.firstOrNull { it.name == wanted },
                onReport = { nav.navigate("report") },
                bottomBar = { reporterBar(2) }
            )
        }
        composable("details/{id}") { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            ReportDetailsScreen(
                report = reports.firstOrNull { it.id == id },
                role = role,
                userName = userName,
                onBack = { nav.popBackStack() },
                onStatus = { status, text -> vm.updateStatus(id, status, text) },
                onNote = { text -> vm.addNote(id, text) },
                onEscalate = { vm.escalate(id) },
                onAssign = { technician -> vm.assign(id, technician) },
                loadPhoto = { reportId -> vm.loadPhoto(reportId) }
            )
        }
        composable("dashboard") {
            MaintenanceDashboardScreen(
                role = role,
                userName = userName,
                reports = reports,
                tab = JobsTab.ALL,
                onOpen = openDetails,
                onNewReport = { nav.navigate("report") },
                onLogout = logout,
                onSeed = seed,
                bottomBar = { if (role == Role.SUPERVISOR) supervisorBar(0) }
            )
        }
        composable("unassigned") {
            MaintenanceDashboardScreen(
                role = role,
                userName = userName,
                reports = reports,
                tab = JobsTab.UNASSIGNED,
                onOpen = openDetails,
                onNewReport = { nav.navigate("report") },
                onLogout = logout,
                bottomBar = { supervisorBar(1) }
            )
        }
    }
}