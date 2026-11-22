package com.example.campusmaintenancereporter


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ReportFormScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var submitted: NewReport? = null

    private fun show() {
        submitted = null
        rule.setContent {
            MaintenanceTheme {
                ReportFormScreen(
                    onBack = {},
                    onSubmit = { submitted = it; "CMR-9999" },
                    onDone = {}
                )
            }
        }
    }

    private fun pickBuilding(name: String) {
        rule.onNodeWithText("Building or hostel").performClick()
        rule.onNodeWithText(name).performClick()
    }

    private fun pickCategory(name: String) {
        rule.onNodeWithText("Issue type").performScrollTo().performClick()
        rule.onNodeWithText(name).performClick()
    }

    private fun type(label: String, text: String) {
        rule.onNodeWithText(label).performScrollTo().performTextInput(text)
    }

    private fun submit() {
        rule.onNodeWithText("Submit report").performScrollTo().performClick()
    }

    private fun fillForm(
        building: Boolean = true,
        category: Boolean = true,
        title: String = "Leaking tap",
        description: String = "The tap drips all day"
    ) {
        if (building) pickBuilding("Library")
        if (category) pickCategory("Plumbing")
        if (title.isNotEmpty()) type("Title (e.g. Leaking tap)", title)
        if (description.isNotEmpty()) type("Describe the problem", description)
    }

    private fun assertNothingSubmitted() {
        rule.onNodeWithText("Report submitted").assertDoesNotExist()
        assertNull(submitted)
    }

    @Test
    fun missingBuildingBlocksSubmission() {
        show(); fillForm(building = false); submit(); assertNothingSubmitted()
    }

    @Test
    fun missingIssueTypeBlocksSubmission() {
        show(); fillForm(category = false); submit(); assertNothingSubmitted()
    }

    @Test
    fun missingTitleBlocksSubmission() {
        show(); fillForm(title = ""); submit(); assertNothingSubmitted()
    }

    @Test
    fun spacesOnlyTitleAndDescriptionAreRejected() {
        show(); fillForm(title = "   ", description = "   "); submit(); assertNothingSubmitted()
    }

    @Test
    fun targetFixTimeFollowsThePickedPriority() {
        show()
        rule.onNodeWithText("Target fix time: 5 days", substring = true).performScrollTo().assertExists()
        rule.onNodeWithText("Urgent").performScrollTo().performClick()
        rule.onNodeWithText("Target fix time: 1 day", substring = true).assertExists()
    }

    @Test
    fun emptyFormCannotBeSubmitted() {
        show()
        submit()
        rule.onNodeWithText("Report submitted").assertDoesNotExist()
        assertNull(submitted)
    }

    @Test
    fun missingDescriptionBlocksSubmission() {
        show()
        pickBuilding("Library")
        pickCategory("Plumbing")
        type("Title (e.g. Leaking tap)", "Leaking tap")
        submit()
        rule.onNodeWithText("Report submitted").assertDoesNotExist()
        assertNull(submitted)
    }

    @Test
    fun completeFormIsSubmittedAndShowsTheReference() {
        show()
        pickBuilding("Library")
        pickCategory("Plumbing")
        type("Title (e.g. Leaking tap)", "Leaking tap")
        type("Describe the problem", "The tap drips all day")
        submit()

        rule.onNodeWithText("Report submitted").assertIsDisplayed()
        rule.onNodeWithText("CMR-9999").assertIsDisplayed()

        val report = submitted
        assertNotNull(report)
        assertEquals("Library", report!!.location)
        assertEquals("Plumbing", report.category)
        assertEquals("Leaking tap", report.title)
        assertEquals(Priority.MEDIUM, report.priority)
    }

    @Test
    fun locationCombinesBuildingBlockAndRoom() {
        show()
        pickBuilding("Library")
        type("Block", "Block 2")
        type("Room", "14")
        pickCategory("Plumbing")
        type("Title (e.g. Leaking tap)", "Leaking tap")
        type("Describe the problem", "The tap drips all day")
        submit()
        assertEquals("Library, Block 2, 14", submitted!!.location)
    }

    @Test
    fun safetyCategoryShowsTheEmergencyNoticeAndRaisesPriority() {
        show()
        pickCategory("Safety")
        rule.onNodeWithText("Is it an emergency?").assertExists()
        rule.onNodeWithText(Priority.HIGH.hint, substring = true).assertExists()
    }

    @Test
    fun choosingAPriorityShowsItsHint() {
        show()
        rule.onNodeWithText("Urgent").performScrollTo().performClick()
        rule.onNodeWithText(Priority.URGENT.hint, substring = true).assertExists()
    }

    @Test
    fun photoOptionsAreOffered() {
        show()
        rule.onNodeWithText("Take photo").performScrollTo().assertExists()
        rule.onNodeWithText("From gallery").performScrollTo().assertExists()
        rule.onNodeWithText("Please do not photograph people.").assertExists()
    }
}
