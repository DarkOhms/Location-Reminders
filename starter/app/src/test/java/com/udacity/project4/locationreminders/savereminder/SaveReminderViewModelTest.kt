package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SaveReminderViewModelTest {

    private lateinit var dataSource: FakeDataSource
    private lateinit var remindersList: MutableList<ReminderDTO>
    private lateinit var viewModel: SaveReminderViewModel

    //TODO: provide testing to the SaveReminderView and its live data objects
    /*
    @get:Rule
    var instantTaskExecutorRule: InstantTaskExecutorRule()

     */

            @Before
            fun setupViewModel(){

                //setup viewModel with 3 items
                val reminder1 = ReminderDTO("Get lunch", "remember to eat your veggies", "home", randomLatOrLong(), randomLatOrLong())
                val reminder2 = ReminderDTO("Check tires", "make sure tire pressure is approx 33psi", "gas station", randomLatOrLong(), randomLatOrLong())
                val reminder3 = ReminderDTO("get swole", "minimum 14 sets and 15 minutes of zone 2", "gym", randomLatOrLong(), randomLatOrLong())

                remindersList = mutableListOf(reminder1, reminder2, reminder3)
                dataSource = FakeDataSource(remindersList)

                viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
            }

    @Test
    fun getTasks_requestsAllTasksFromRemoteDataSource() = runTest{
        // When reminders are requested from the DataSource
        val reminders = dataSource.getReminders() as Result.Success

        // Then tasks are loaded from the remote data source
        assertThat(reminders.data, IsEqual(remindersList))
    }

    fun randomLatOrLong(): Double {
        val min = -180.0 // Minimum allowed value (longitude)
        val max = 180.0  // Maximum allowed value (longitude, can be adjusted for latitude)

        // Generate a random double between min and max (exclusive)
        return Random.nextDouble(min, max)
    }

}