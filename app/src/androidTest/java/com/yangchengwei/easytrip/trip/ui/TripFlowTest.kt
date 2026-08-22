package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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

    @Test fun formalNavigationAndGenericDayOperations() {
        val repository = FakeTripRepository().apply { seed("川西环线", 4) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }
        compose.onNodeWithText("川西环线").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("旅行工作区 id-1").assertIsDisplayed()
        compose.onNodeWithText("旅行设置").performClick()
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("旅行工作区 id-1").assertIsDisplayed()
        compose.onNodeWithText("旅行设置").performClick()
        compose.onNodeWithText("操作 Day 2").performClick()
        compose.onNodeWithText("前插").performClick()
        compose.waitForIdle()
        assertEquals(5, repository.trip.value!!.days.size)
        compose.onNodeWithText("末尾追加旅行日").performClick()
        compose.waitForIdle()
        assertEquals(6, repository.trip.value!!.days.size)
    }

    @Test fun dragMovesAcrossTwoPositionsOnceAndAccessibilityMovesOnePosition() {
        val repository = FakeTripRepository().apply { seed("拖动测试", 5) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }
        compose.onNodeWithText("设置 拖动测试").performClick()
        val originalIds = repository.trip.value!!.days.map { it.id }

        compose.onNodeWithText("拖动 Day 1").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(Offset(center.x, center.y + 400f), 500)
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(MoveCall(originalIds[0], 2)), repository.moveCalls)
        assertEquals(
            listOf(originalIds[1], originalIds[2], originalIds[0], originalIds[3], originalIds[4]),
            repository.trip.value!!.days.map { it.id },
        )

        compose.onNodeWithTag("day-row-${originalIds[1]}", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .first { it.label == "下移" }
            .action()
        compose.waitForIdle()

        assertEquals(MoveCall(originalIds[1], 1), repository.moveCalls.last())
        assertEquals(2, repository.moveCalls.size)
    }

    @Test fun cancellingDeleteDialogsDoesNotDeleteAnything() {
        val repository = FakeTripRepository().apply { seed("取消删除测试", 3) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithText("删除 取消删除测试").performClick()
        compose.onNodeWithText("旅行日 3，地点 2，标签 1，行程项 4，路线段 5").assertIsDisplayed()
        compose.onNodeWithText("取消删除旅行").performClick()
        assertEquals(0, repository.deletedTrips.size)

        compose.onNodeWithText("设置 取消删除测试").performClick()
        compose.onNodeWithText("操作 Day 1").performClick()
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("行程项 2，路线段 1").assertIsDisplayed()
        compose.onNodeWithText("取消删除旅行日").performClick()
        assertEquals(0, repository.deletedDays)
    }

    @Test fun confirmingTripDeleteShowsImpactAndDeletesExactlyOnce() {
        val repository = FakeTripRepository().apply { seed("确认删除测试", 3) }
        val tripId = repository.trip.value!!.id
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithText("删除 确认删除测试").performClick()
        compose.onNodeWithText("旅行日 3，地点 2，标签 1，行程项 4，路线段 5").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行").performClick()

        compose.waitUntil { repository.trip.value == null }
        assertEquals(listOf(tripId), repository.deletedTrips)
        assertEquals(0, compose.onAllNodesWithText("确认删除测试").fetchSemanticsNodes().size)
    }

    @Test fun confirmingDayDeleteDeletesExactlyOnce() {
        val repository = FakeTripRepository().apply { seed("删除旅行日测试", 3) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }
        compose.onNodeWithText("设置 删除旅行日测试").performClick()
        compose.onNodeWithText("操作 Day 1").performClick()
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("行程项 2，路线段 1").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行日").performClick()
        compose.waitForIdle()
        assertEquals(1, repository.deletedDays)
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
        compose.onNodeWithText("创建旅行").performClick(); compose.onNodeWithText("旅行名称").performTextInput("日期旅行"); compose.onNodeWithText("天数").performTextInput("2"); compose.onNodeWithText("指定日期").performClick(); compose.onNodeWithText("选择起始日期").performClick()
        compose.onNodeWithText("确定日期").performClick()
        compose.onNodeWithText("自驾").performClick()
        compose.onNodeWithText("自驾").assertIsSelected()
        compose.onNodeWithText("创建").performClick()
        compose.waitUntil { repository.trip.value?.name == "日期旅行" }
        assertEquals(TravelMode.SELF_DRIVE, repository.trip.value!!.travelMode)
        assertEquals(LocalDate.of(2027, 3, 15), repository.trip.value!!.startDate)
    }

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
