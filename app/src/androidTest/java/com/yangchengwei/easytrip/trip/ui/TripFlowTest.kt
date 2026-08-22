package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settingsShowsTravelFieldsAndRenamesFromCenteredTitle() {
        val repository = FakeTripRepository().apply { seed("川西环线", 4) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithTag("trip-settings-id-1").performClick()
        compose.onNodeWithText("川西环线").performClick()
        compose.onNodeWithTag("rename-input").performTextInput("新名称")
        compose.onNodeWithText("保存名称").performClick()
        compose.onNodeWithText("出行日期").assertIsDisplayed()
        compose.onNodeWithText("出行方式").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("末尾追加旅行日").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("操作 Day 1").fetchSemanticsNodes().size)
    }

    @Test fun confirmingTripDeleteShowsImpactAndDeletesExactlyOnce() {
        val repository = FakeTripRepository().apply { seed("确认删除测试", 3) }
        val tripId = repository.trip.value!!.id
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithTag("trip-delete-id-1").performClick()
        compose.onNodeWithText("旅行日 3，地点 2，标签 1，行程项 4，路线段 5").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行").performClick()

        compose.waitUntil { repository.trip.value == null }
        assertEquals(listOf(tripId), repository.deletedTrips)
    }

    @Test fun createAndSettingsChoicesUseExclusiveSelectablePills() {
        val repository = FakeTripRepository().apply { seed("样式测试", 1) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }
        compose.onNodeWithText("创建旅行").performClick()
        compose.onNodeWithTag("create-time-DRAFT").assert(hasRole(Role.RadioButton)).assertIsSelected()
        compose.onNodeWithTag("create-time-DATED").assertIsNotSelected()
        compose.onNodeWithTag("create-mode-FLEXIBLE").assertIsSelected()
        compose.onNodeWithTag("create-mode-SELF_DRIVE").assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithText("确定日期").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("选择起始日期").fetchSemanticsNodes().size)
        pressBack()
        compose.onNodeWithTag("create-time-DRAFT").assertIsSelected()
        compose.onNodeWithTag("create-time-DATED").assertIsNotSelected()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("trip-settings-id-1").performClick()
        compose.onNodeWithTag("settings-mode-FLEXIBLE").assert(hasRole(Role.RadioButton)).assertIsSelected()
        compose.onNodeWithTag("settings-mode-SELF_DRIVE").assertIsNotSelected()
    }

    @Test fun createsThreeDayDraftAndDatedSelfDrive() {
        val repository = FakeTripRepository()
        val march15 = LocalDate.of(2027, 3, 15)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        compose.setContent {
            AppNavigation(
                TripService(repository),
                repository,
                FakeImpacts(),
                march15,
            )
        }
        compose.onNodeWithText("创建旅行").performClick(); compose.onNodeWithText("旅行名称").performTextInput("草案"); compose.onNodeWithText("天数").performTextInput("3"); compose.onNodeWithText("创建").performClick()
        compose.waitForIdle(); assertEquals(3, repository.trip.value!!.days.size)
        compose.onNodeWithText("创建旅行").performClick(); compose.onNodeWithText("旅行名称").performTextInput("日期旅行"); compose.onNodeWithText("天数").performTextInput("2"); compose.onNodeWithText("指定日期").performClick()
        compose.onNodeWithText("确定日期").performClick()
        compose.onNodeWithText("2027-03-15").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("无日期").performClick()
        compose.onNodeWithText("指定日期").assertIsDisplayed().assertIsNotSelected()
        compose.onNodeWithText("指定日期").performClick()
        compose.onNodeWithText("确定日期").performClick()
        compose.onNodeWithText("2027-03-15").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("自驾").performClick()
        compose.onNodeWithText("自驾").assertIsSelected()
        compose.onNodeWithText("创建").performClick()
        compose.waitUntil { repository.trip.value?.name == "日期旅行" }
        assertEquals(TravelMode.SELF_DRIVE, repository.trip.value!!.travelMode)
        assertEquals(LocalDate.of(2027, 3, 15), repository.trip.value!!.startDate)
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private class FakeImpacts : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = TripDeleteImpact(3, 2, 1, 4, 5)
        override suspend fun day(dayId: String) = DayDeleteImpact(2, 1)
    }

    private data class MoveCall(val dayId: String, val targetIndex: Int)

    private class FakeTripRepository : TripRepository {
        private var nextId = 1
        val trip = MutableStateFlow<TripWithDays?>(null)
        private val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        val moveCalls = mutableListOf<MoveCall>()
        val deletedTrips = mutableListOf<String>()
        var deletedDays = 0
        fun seed(name: String, count: Int) { create(CreateTrip(name, count)) }
        private fun create(command: CreateTrip): String { val id=id(); trip.value=TripWithDays(id,command.name,null,command.travelMode,List(command.dayCount){TripDay(id(),it)}); publish(); return id }
        override fun observeTrips(): Flow<List<TripSummary>> = trips
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun createTrip(command: CreateTrip) = create(command)
        override suspend fun renameTrip(tripId: String, name: String) {}
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) { trip.value=trip.value!!.copy(startDate=startDate); publish() }
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) { trip.value=trip.value!!.copy(travelMode=mode); publish() }
        override suspend fun insertDay(tripId:String,anchorDayId:String?,side:InsertSide):String { val d=trip.value!!.days.toMutableList();val id=id();val a=anchorDayId?.let{v->d.indexOfFirst{it.id==v}}?:d.size;d.add((a+if(side==InsertSide.AFTER)1 else 0).coerceIn(0,d.size),TripDay(id,0));setDays(d);return id }
        override suspend fun moveDay(tripId:String,dayId:String,targetIndex:Int){moveCalls += MoveCall(dayId,targetIndex);val d=trip.value!!.days.toMutableList();val day=d.removeAt(d.indexOfFirst{it.id==dayId});d.add(targetIndex,day);setDays(d)}
        override suspend fun deleteDay(dayId:String){deletedDays++;setDays(trip.value!!.days.filterNot{it.id==dayId})}
        override suspend fun deleteTrip(tripId:String){deletedTrips += tripId;trip.value=null;trips.value=emptyList()}
        private fun setDays(d:List<TripDay>){trip.value=trip.value!!.copy(days=d.mapIndexed{i,x->x.copy(index=i)});publish()}
        private fun publish(){trip.value?.let{trips.value=listOf(TripSummary(it.id,it.name,it.startDate,it.travelMode,it.days.size))}}
        private fun id()="id-${nextId++}"
    }
}
