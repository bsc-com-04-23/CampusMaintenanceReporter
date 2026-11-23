package com.example.campusmaintenancereporter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var result: Pair<String, Role>? = null

    private fun show() {
        result = null
        rule.setContent {
            MaintenanceTheme {
                LoginScreen(
                    authenticate = { email, password -> DemoAuthService().authenticate(email, password) },
                    onLogin = { s -> result = s.email to s.role }
                )
            }
        }
    }

    private fun signIn(email: String, password: String) {
        rule.onNodeWithText("Email or registration number").performTextInput(email)
        rule.onNodeWithText("Password").performTextInput(password)
        rule.onNodeWithText("Sign in").performClick()
    }

    @Test
    fun signInIsDisabledUntilBothFieldsAreFilled() {
        show()
        rule.onNodeWithText("Sign in").assertIsNotEnabled()
        rule.onNodeWithText("Email or registration number").performTextInput("student@campus.test")
        rule.onNodeWithText("Sign in").assertIsNotEnabled()
        rule.onNodeWithText("Password").performTextInput("student123")
        rule.onNodeWithText("Sign in").assertIsEnabled()
    }

    @Test
    fun wrongPasswordShowsTheGenericError() {
        show()
        signIn("student@campus.test", "wrongpass")
        rule.onNodeWithText("Incorrect email or password.").assertIsDisplayed()
        assertNull(result)
    }

    @Test
    fun errorDisappearsWhenTheUserTypesAgain() {
        show()
        signIn("student@campus.test", "wrongpass")
        rule.onNodeWithText("Incorrect email or password.").assertIsDisplayed()
        rule.onNodeWithText("Password").performTextInput("x")
        rule.onNodeWithText("Incorrect email or password.").assertDoesNotExist()
    }

    @Test
    fun studentCredentialsGiveTheStudentRole() {
        show()
        signIn("student@campus.test", "student123")
        assertEquals("student@campus.test" to Role.STUDENT, result)
    }

    @Test
    fun supervisorCredentialsGiveTheSupervisorRole() {
        show()
        signIn("supervisor@campus.test", "super123")
        assertEquals("supervisor@campus.test" to Role.SUPERVISOR, result)
    }

    @Test
    fun emailIsTrimmedAndLowercased() {
        show()
        signIn("  Student@Campus.Test ", "student123")
        assertEquals("student@campus.test" to Role.STUDENT, result)
    }

    @Test
    fun showAndHideButtonTogglesItsLabel() {
        show()
        rule.onNodeWithText("Show", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Hide", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Hide", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Show", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun forgotPasswordOpensAndClosesAHelpDialog() {
        show()
        rule.onNodeWithText("Forgot password?").performClick()
        rule.onNodeWithText("Forgot your password?").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        rule.onNodeWithText("Forgot your password?").assertDoesNotExist()
    }
}