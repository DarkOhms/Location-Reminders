package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.randomLatOrLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext<Application>()
        repository = FakeAndroidDataSource()


        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            // fake data source
            // RemindersListViewModel
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            // SaveReminderViewModel
            single {
                //This view model is declared singleton to be used across multiple fragments
                SaveReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single {repository }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun tearDown(){
        stopKoin()
    }

    @Test
    fun clickAdd_navigateToSaveReminderFragment(){
        //GIVEN we are logged in and on the ReminderListFragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment{
            Navigation.setViewNavController(it.view!!, navController)
        }
        //WHEN clicking the add icon
        onView(withId(R.id.addReminderFAB)).perform(click())

        //THEN we navigate to SaveReminderFragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
    @Test
    fun reminderAdded_DisplayedInUi() = runBlockingTest{
        // GIVEN - a repository, RemindersListViewModel, and SaveReminderViewModel saves a reminder
        val reminder = ReminderDTO("reminder", "remember me!", "some place", randomLatOrLong(), randomLatOrLong())
        repository.saveReminder(reminder)

        // WHEN - remindersListViewModel loads reminders on the ReminderListFragment screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - the reminder is displayed in the list
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(hasDescendant(withText(reminder.title))))
    }

    @Test
    fun whenErrorCondition_ToastDisplayedInUi() = runBlockingTest {
        //GIVEN an error condition
        (repository as FakeAndroidDataSource).shouldReturnError = true

        //WHEN the reminders list tries to load
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //THEN a snackbar message will appear
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("I am an error for Android test") ))

    }

}