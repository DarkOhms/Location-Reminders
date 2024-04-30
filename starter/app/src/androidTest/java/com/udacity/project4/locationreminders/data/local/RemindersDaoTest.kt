package com.udacity.project4.locationreminders.data.local

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.randomLatOrLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runTest {
        //GIVEN a reminder is inserted
        val reminder = ReminderDTO("title", "description", "location", randomLatOrLong(), randomLatOrLong() )
        Log.d("ReminderID", reminder.id)
        database.reminderDao().saveReminder(reminder)

        //WHEN we get a reminder by ID from the database
        val loaded = database.reminderDao().getReminderById(reminder.id)

        //THEN the loaded data returns the expected values
        MatcherAssert.assertThat<ReminderDTO>(loaded as ReminderDTO, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(loaded.id, CoreMatchers.`is` (reminder.id))
        MatcherAssert.assertThat(loaded.title, CoreMatchers.`is` (reminder.title))
        MatcherAssert.assertThat(loaded.description, CoreMatchers.`is` (reminder.description))
    }

    @Test
    fun deleteReminders() = runTest {
        //GIVEN a reminder is inserted
        val reminder = ReminderDTO("title", "description", "location", randomLatOrLong(), randomLatOrLong() )
        Log.d("ReminderID", reminder.id)
        database.reminderDao().saveReminder(reminder)

        //WHEN we delete reminders from the database
        database.reminderDao().deleteAllReminders()
        val result = database.reminderDao().getReminders()

        //THEN the loaded data returns an empty list
        MatcherAssert.assertThat<List<ReminderDTO>>(result as List<ReminderDTO>, CoreMatchers.`is` (emptyList()))

    }

    @Test
    fun getReminders_whenEmpty_shouldReturnEmptyLIst() = runTest {
        //GIVEN no reminders are inserted

        //WHEN we getReminders
        val result = database.reminderDao().getReminders()

        //THEN the loaded data returns an empty list
        MatcherAssert.assertThat<List<ReminderDTO>>(result as List<ReminderDTO>, CoreMatchers.`is` (emptyList()))

    }


}