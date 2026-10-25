package com.example.campusmaintenancereporter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoAccountsTest {

    @Test
    fun everyAccountLogsInWithItsOwnRole() {
        assertEquals(Role.STUDENT, DemoAccounts.login("student@campus.test", "student123"))
        assertEquals(Role.STAFF, DemoAccounts.login("staff@campus.test", "staff123"))
        assertEquals(Role.MAINTENANCE, DemoAccounts.login("maintenance@campus.test", "maint123"))
        assertEquals(Role.MAINTENANCE, DemoAccounts.login("phiri@campus.test", "phiri123"))
        assertEquals(Role.MAINTENANCE, DemoAccounts.login("mussa@campus.test", "mussa123"))
        assertEquals(Role.SUPERVISOR, DemoAccounts.login("supervisor@campus.test", "super123"))
    }

    @Test
    fun wrongPasswordIsRejected() {
        assertNull(DemoAccounts.login("supervisor@campus.test", "wrong"))
    }

    @Test
    fun unknownEmailIsRejected() {
        assertNull(DemoAccounts.login("nobody@campus.test", "student123"))
    }

    @Test
    fun emptyCredentialsAreRejected() {
        assertNull(DemoAccounts.login("", ""))
        assertNull(DemoAccounts.login("student@campus.test", ""))
        assertNull(DemoAccounts.login("", "student123"))
    }

    @Test
    fun emailIgnoresCapitalsAndSurroundingSpaces() {
        assertEquals(Role.STUDENT, DemoAccounts.login("  STUDENT@Campus.Test ", "student123"))
    }

    @Test
    fun passwordIsCaseSensitive() {
        assertNull(DemoAccounts.login("student@campus.test", "Student123"))
    }

    @Test
    fun aStudentPasswordDoesNotWorkForAnotherAccount() {
        assertNull(DemoAccounts.login("supervisor@campus.test", "student123"))
    }

    @Test
    fun namesComeFromTheAccount() {
        assertEquals("J. Banda", DemoAccounts.nameOf("maintenance@campus.test"))
        assertEquals("Supervisor", DemoAccounts.nameOf("supervisor@campus.test"))
        assertEquals("someone", DemoAccounts.nameOf("someone@else.test"))
    }
}