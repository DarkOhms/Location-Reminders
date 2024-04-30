package com.udacity.project4.locationreminders.reminderslist

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    private lateinit var dataSource: FakeDataSource
    private lateinit var remindersList: MutableList<ReminderDTO>
    private lateinit var viewModel: RemindersListViewModel


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

        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun load_reminders_check_loading() = mainCoroutineRule.runBlockingTest{
        //GIVEN a viewmodel with data

        //WHEN load reminders is called
        mainCoroutineRule.dispatcher.pauseDispatcher()
        viewModel.loadReminders()

        //THEN the LiveData isLoading is true
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), Is.`is`(true))
        mainCoroutineRule.dispatcher.resumeDispatcher()
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), Is.`is`(false))

    }

    fun randomLatOrLong(): Double {
        val min = -180.0 // Minimum allowed value (longitude)
        val max = 180.0  // Maximum allowed value (longitude, can be adjusted for latitude)

        // Generate a random double between min and max (exclusive)
        return Random.nextDouble(min, max)
    }

}