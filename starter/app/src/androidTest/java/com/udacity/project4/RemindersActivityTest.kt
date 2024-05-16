package com.udacity.project4

import android.Manifest
import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.DataBindingIdlingResource
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var activityContext: Activity
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */

    @Rule
    @JvmField
    var mRuntimePermissionRuleFine: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResources(){
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResources(){
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


//    TODO: add End to End testing to the app
    @Test
    fun createReminder_confirmReminderDisplays(){
        val stringToTest = "Remember This!"


        //GIVEN a user is logged in with the relevant permissions and location on

        //WHEN the user clicks on the add button and goes through the process to add a reminder
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        activityScenario.onActivity { activity ->
            activityContext = activity
        }
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //navigate to the SaveDetailsFragment
        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        //enter test title and description
        onView(withId(R.id.reminderTitle)).perform(typeText(stringToTest))
        onView(withId(R.id.reminderDescription)).perform(typeText("I'm so glad you remember!"))
        closeSoftKeyboard()


        //navigate to select location
        onView(withId(R.id.selectLocation)).perform(click())

        //simulate click
        Thread.sleep(2000L)
        val device = UiDevice.getInstance(getInstrumentation())

        device.click(300.506484068931.toInt(), 300.4435350385924.toInt())
        Thread.sleep(2000L)

        //navigate back to save location
        onView(withId(R.id.saveLocationButton)).perform(click())

        //save the reminder
        onView(withId(R.id.saveReminder)).perform(click())

        Thread.sleep(2000L)


        //THEN the same reminder is displayed in the reminders list and a toast displayed
        onView(withText(stringToTest)).check(matches(isDisplayed()))
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(activityContext.window.decorView))).check(matches(isDisplayed()))


    activityScenario.close()
    }

}

