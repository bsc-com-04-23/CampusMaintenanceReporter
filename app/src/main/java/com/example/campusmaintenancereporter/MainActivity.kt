package com.example.campusmaintenancereporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
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
fun AppNav(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val session by vm.session.collectAsState()
    val reports by vm.visibleReports.collectAsState()
    val role = session?.role ?: Role.STUDENT
    val userName = session?.let { DemoAccounts.nameOf(it.email) } ?: ""

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

    val jobsBar: @Composable (Int) -> Unit = { selected ->
        val second = if (role == Role.SUPERVISOR) {
            NavTab("Unassigned", Icons.Default.Warning) { goTab("unassigned", "dashboard") }
        } else {
            NavTab("My Jobs", Icons.Default.Person) { goTab("myjobs", "dashboard") }
        }
        AppBottomBar(
            tabs = listOf(
                NavTab("All Jobs", Icons.AutoMirrored.Filled.List) { goTab("dashboard", "dashboard") },
                second
            ),
            selected = selected
        )
    }

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen { email, pickedRole ->
                vm.signIn(email, pickedRole)
                nav.navigate(if (pickedRole.isMaintenance) "dashboard" else "home") {
                    popUpTo("login") { inclusive = true }
                }
            }
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
                onSubmit = { vm.submitReport(it) },
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
                onAssign = { technician -> vm.assign(id, technician) }
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
                bottomBar = { jobsBar(0) }
            )
        }
        composable("myjobs") {
            MaintenanceDashboardScreen(
                role = role,
                userName = userName,
                reports = reports,
                tab = JobsTab.MINE,
                onOpen = openDetails,
                onNewReport = { nav.navigate("report") },
                onLogout = logout,
                bottomBar = { jobsBar(1) }
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
                bottomBar = { jobsBar(1) }
            )
        }
    }
}