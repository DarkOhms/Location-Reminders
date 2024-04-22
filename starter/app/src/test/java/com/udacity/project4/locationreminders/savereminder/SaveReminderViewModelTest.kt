package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.toReminderDataItem
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(sdk = [32])
class SaveReminderViewModelTest {

    private lateinit var dataSource: FakeDataSource
    private lateinit var remindersList: MutableList<ReminderDTO>
    private lateinit var viewModel: SaveReminderViewModel

    //TODO: provide testing to the SaveReminderView and its live data objects



    //@get:Rule
    //val instantTaskExecutorRule = InstantTaskExecutorRule()


    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()



    @Before
    fun setup(){
        setupViewModel()
    }


    fun setupViewModel(){

                //setup viewModel with 3 items
                val reminder1 = ReminderDTO("Get lunch", "remember to eat your veggies", "home", randomLatOrLong(), randomLatOrLong())
                val reminder2 = ReminderDTO("Check tires", "make sure tire pressure is approx 33psi", "gas station", randomLatOrLong(), randomLatOrLong())
                val reminder3 = ReminderDTO("get swole", "minimum 14 sets and 15 minutes of zone 2", "gym", randomLatOrLong(), randomLatOrLong())

                remindersList = mutableListOf(reminder1, reminder2, reminder3)
                dataSource = FakeDataSource(remindersList)

                //val application = Mockito.mock(Application::class.java)

                viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
            }

    @Test
    fun getReminders_requestsAllRemindersFromDataSource() = mainCoroutineRule.runBlockingTest{
        // When reminders are requested from the DataSource
        val reminders = dataSource.getReminders() as Result.Success

        // Then reminders are loaded from the data source
        assertThat(reminders.data, IsEqual(remindersList))
    }


    @Test
    @ExperimentalCoroutinesApi
    fun check_loading() = mainCoroutineRule.runBlockingTest{
        //GIVEN a viewmodel with data

        //WHEN a reminder is saved
        val reminder = ReminderDTO("new reminder", "remember this", "somewhere", randomLatOrLong(), randomLatOrLong())
        val reminderDataItem = reminder.toReminderDataItem()

        mainCoroutineRule.dispatcher.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminderDataItem)

        //THEN the LiveData isLoading is true
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is` (true))
        mainCoroutineRule.dispatcher.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is` (false))

    }

    fun randomLatOrLong(): Double {
        val min = -180.0 // Minimum allowed value (longitude)
        val max = 180.0  // Maximum allowed value (longitude, can be adjusted for latitude)

        // Generate a random double between min and max (exclusive)
        return Random.nextDouble(min, max)
    }

}