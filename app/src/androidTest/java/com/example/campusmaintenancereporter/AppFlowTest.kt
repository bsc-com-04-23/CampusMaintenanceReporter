package com.example.campusmaintenancereporter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

class AppFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun login(email: String, password: String) {
        rule.onNodeWithText("Email or registration number").performTextInput(email)
        rule.onNodeWithText("Password").performTextInput(password)
        rule.onNodeWithText("Sign in").performClick()
    }

    @Test
    fun supervisorLogsAReportAndSeesItOnTheDashboard() {
        login("supervisor@campus.test", "super123")

        rule.onNodeWithText("Log a report", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Building or hostel").performClick()
        rule.onNodeWithText("Library").performClick()
        rule.onNodeWithText("Issue type").performScrollTo().performClick()
        rule.onNodeWithText("Plumbing").performClick()
        rule.onNodeWithText("Title (e.g. Leaking tap)").performScrollTo().performTextInput("Zzflow report")
        rule.onNodeWithText("Describe the problem").performScrollTo().performTextInput("Phoned in to the desk")
        rule.onNodeWithText("Submit report").performScrollTo().performClick()
        rule.onNodeWithText("Done").performClick()

        rule.onNodeWithText("Search title, location, reference").performTextInput("zzflow")
        rule.onNodeWithText("Zzflow report").assertIsDisplayed()
    }

    @Test
    fun studentLandsOnHome() {
        login("student@campus.test", "student123")
        rule.onNodeWithText("Hello, Student").assertIsDisplayed()
    }

    @Test
    fun wrongPasswordStaysOnTheLoginScreen() {
        login("student@campus.test", "nope")
        rule.onNodeWithText("Incorrect email or password.").assertIsDisplayed()
    }

    @Test
    fun supervisorLandsOnTheSupervisorDashboard() {
        login("supervisor@campus.test", "super123")
        rule.onNodeWithText("Supervisor Dashboard").assertIsDisplayed()
        rule.onNodeWithText("All Jobs").assertIsDisplayed()
        rule.waitForIdle()
        rule.onNodeWithText("Log a report", useUnmergedTree = true).assertExists()
    }

    @Test
    fun technicianLandsOnTheirJobsWithoutLogAReport() {
        login("maintenance@campus.test", "maint123")
        rule.onNodeWithText("My Jobs").assertIsDisplayed()
        rule.onNodeWithText("Log a report", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun technicianSeesOnlyTheJobsAssignedToThem() {
        login("maintenance@campus.test", "maint123")
        rule.onNodeWithText("Leaking tap").assertIsDisplayed()
        rule.onNodeWithText("Broken classroom light").assertDoesNotExist()
        rule.onNodeWithText("Exposed wiring near socket").assertDoesNotExist()
        rule.onNodeWithText("No internet in lab").assertDoesNotExist()
    }

    @Test
    fun studentSubmitsAReportAndFindsItInMyReports() {
        login("student@campus.test", "student123")

        rule.onNodeWithText("Report a Problem").performClick()
        rule.onNodeWithText("Building or hostel").performClick()
        rule.onNodeWithText("Library").performClick()
        rule.onNodeWithText("Issue type").performScrollTo().performClick()
        rule.onNodeWithText("Plumbing").performClick()
        rule.onNodeWithText("Title (e.g. Leaking tap)").performScrollTo().performTextInput("Flow test report")
        rule.onNodeWithText("Describe the problem").performScrollTo().performTextInput("Created by an automated test")
        rule.onNodeWithText("Submit report").performScrollTo().performClick()

        rule.onNodeWithText("Report submitted").assertIsDisplayed()
        rule.onNodeWithText("Done").performClick()

        rule.onAllNodesWithText("My Reports").onFirst().performClick()
        rule.onNodeWithText("Flow test report").assertIsDisplayed()
    }

    @Test
    fun signOutFromTheAccountMenuReturnsToLogin() {
        login("student@campus.test", "student123")
        rule.onNodeWithContentDescription("Account").performClick()
        rule.onNodeWithText("Sign out").performClick()
        rule.onNodeWithText("Stay").assertIsDisplayed()
        rule.onNodeWithText("Sign out").performClick()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun backButtonOnHomeAsksBeforeSigningOut() {
        login("student@campus.test", "student123")
        Espresso.pressBack()
        rule.onNodeWithText("Sign out?").assertIsDisplayed()
        rule.onNodeWithText("Stay").performClick()
        rule.onNodeWithText("Hello, Student").assertIsDisplayed()
    }
}
