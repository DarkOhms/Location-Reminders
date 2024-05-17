package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.randomLatOrLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    TODO: Add testing implementation to the RemindersLocalRepository.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
    // Using an in-memory database for testing, because it doesn't survive killing the process.
    database = Room.inMemoryDatabaseBuilder( ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java )
        .allowMainThreadQueries() .build()

        localDataSource = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun tearDown(){
        database.close()
    }

    @Test
    fun getReminder_retrievesReminder() = runTest {
        // GIVEN - A new task saved in the database.
        val newReminder =
            ReminderDTO("title", "description", "a place", randomLatOrLong(), randomLatOrLong())
        localDataSource.saveReminder(newReminder)

        // WHEN  - Reminder retrieved by ID.
        val result = localDataSource.getReminder(newReminder.id)

        // THEN - Same reminder is returned.

        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`(newReminder.title))
        assertThat(result.data.description, `is`(newReminder.description))

    }

    @Test
    fun getReminder_shouldReturnError() = runTest {
        // GIVEN - A new task saved in the database.
        val newReminder =
            ReminderDTO("title", "description", "a place", randomLatOrLong(), randomLatOrLong())
        localDataSource.saveReminder(newReminder)

        // WHEN  - Reminder retrieved by the wrong ID.
        val result = localDataSource.getReminder("wrong id")

        // THEN - Error is returned.

        assertThat(result.succeeded, `is`(false))
        result as Result.Error
        //assertThat(result.message, `is`())

    }

}