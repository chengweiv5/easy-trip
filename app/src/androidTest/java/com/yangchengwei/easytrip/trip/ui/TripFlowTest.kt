package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settingsShowsTravelFieldsAndRenamesFromCenteredTitle() {
        val repository = FakeTripRepository().apply { seed("川西环线", 4) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithTag("trip-menu-id-1").performClick()
        compose.onNodeWithTag("trip-menu-settings-id-1").performClick()
        compose.onNodeWithText("修改旅行名称").performClick()
        compose.onNodeWithText("旅行名称").performTextInput("新名称")
        compose.onNodeWithText("保存名称").performClick()
        compose.onNodeWithText("出行日期").assertIsDisplayed()
        compose.onNodeWithText("出行方式").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("末尾追加旅行日").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("操作 Day 1").fetchSemanticsNodes().size)
    }

    @Test fun tripMenuDeleteShowsExactImpactAndDeletesOnce() {
        val repository = FakeTripRepository().apply { seed("确认删除测试", 3) }
        val tripId = repository.trip.value!!.id
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithTag("trip-menu-id-1").performClick()
        compose.onNodeWithTag("trip-menu-delete-id-1").performClick()
        compose.onNodeWithText("删除确认删除测试？").assertIsDisplayed()
        compose.onNodeWithText("此操作将永久删除旅行及其中的所有内容，无法撤销。").assertIsDisplayed()
        compose.onNodeWithText("将删除").assertIsDisplayed()
        compose.onNodeWithText("3 个旅行日").assertIsDisplayed()
        compose.onNodeWithText("2 个收藏地点").assertIsDisplayed()
        compose.onNodeWithText("1 个标签").assertIsDisplayed()
        compose.onNodeWithText("4 个行程项").assertIsDisplayed()
        compose.onNodeWithText("5 个路线段").assertIsDisplayed()
        compose.onNodeWithText("将保留").assertIsDisplayed()
        compose.onNodeWithText("其他旅行及其内容").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行").performClick()

        compose.waitUntil { repository.trip.value == null }
        assertEquals(listOf(tripId), repository.deletedTrips)
    }

    @Test fun exhaustedDeletionSyncShowsReachableResyncAndDoesNotDeleteAgain() {
        val repository = FakeTripRepository().apply {
            seed("同步恢复测试", 3)
            failCollectorsAfterDelete = 2
        }
        val tripId = repository.trip.value!!.id
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }

        compose.onNodeWithTag("trip-menu-id-1").performClick()
        compose.onNodeWithTag("trip-menu-delete-id-1").performClick()
        compose.onNodeWithText("确认删除旅行").performClick()

        compose.onNodeWithText("删除成功，但同步确认失败，请重新同步").assertIsDisplayed()
        compose.onNodeWithText("重新同步").assertIsDisplayed().performClick()
        compose.waitUntil { compose.onAllNodesWithText("删除同步恢复测试？").fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf(tripId), repository.deletedTrips)
    }

    @Test fun impactLoadingAndFailureKeepDeleteTargetVisible() {
        val repository = FakeTripRepository().apply { seed("影响查询测试", 3) }
        val impact = CompletableDeferred<TripDeleteImpact>()
        compose.setContent {
            AppNavigation(
                TripService(repository),
                repository,
                object : DeleteImpactProvider {
                    override suspend fun trip(tripId: String) = impact.await()
                    override suspend fun day(dayId: String) = DayDeleteImpact(0, 0, 0)
                },
            )
        }

        compose.onNodeWithTag("trip-menu-id-1").performClick()
        compose.onNodeWithTag("trip-menu-delete-id-1").performClick()
        compose.onNodeWithText("删除影响查询测试？").assertIsDisplayed()
        compose.onNodeWithText("正在查询删除影响…").assertIsDisplayed()
        compose.onNodeWithTag("trip-delete-impact-loading").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
        compose.onNodeWithText("取消").assertIsNotEnabled().performClick()
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText("删除影响查询测试？").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("0 个旅行日").fetchSemanticsNodes().size)

        compose.runOnIdle { impact.completeExceptionally(IllegalStateException("unavailable")) }
        compose.onNodeWithText("未删除旅行").assertIsDisplayed()
        compose.onNodeWithText("无法加载删除影响，请重试").assertIsDisplayed()
        compose.onNodeWithText("删除影响查询测试？").assertIsDisplayed()
    }

    @Test fun createAndSettingsChoicesUseExclusiveSelectablePills() {
        val repository = FakeTripRepository().apply { seed("样式测试", 1) }
        compose.setContent { AppNavigation(TripService(repository), repository, FakeImpacts()) }
        compose.onNodeWithTag("create-trip").performClick()
        compose.onNodeWithTag("create-time-DRAFT").assert(hasRole(Role.RadioButton)).assertIsSelected()
        compose.onNodeWithTag("create-time-DATED").assertIsNotSelected()
        compose.onNodeWithTag("create-mode-FLEXIBLE").assertIsSelected()
        compose.onNodeWithTag("create-mode-SELF_DRIVE").assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithText("确定日期").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("选择起始日期").fetchSemanticsNodes().size)
        pressBack()
        compose.onNodeWithTag("create-time-DRAFT").assertIsNotSelected()
        compose.onNodeWithTag("create-time-DATED").assertIsSelected()
        pressBack()
        compose.onNodeWithTag("trip-menu-id-1").performClick()
        compose.onNodeWithTag("trip-menu-settings-id-1").performClick()
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
        compose.onNodeWithTag("create-trip").performClick(); compose.onNodeWithTag("create-name").performTextInput("草案"); compose.onNodeWithTag("create-day-count").performTextInput("3"); compose.onNodeWithTag("create-submit").performClick()
        compose.waitUntil { repository.trip.value?.name == "草案" }
        assertEquals(3, repository.trip.value!!.days.size)
        compose.onNodeWithText("旅行工作区 ${repository.trip.value!!.id}").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("create-trip").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("create-trip").performClick(); compose.onNodeWithTag("create-name").performTextInput("日期旅行"); compose.onNodeWithTag("create-day-count").performTextInput("2")
        compose.onNodeWithTag("create-mode-SELF_DRIVE").performScrollTo().performClick()
        compose.onNodeWithText("指定日期").performClick()
        compose.onNodeWithText("确定日期").performClick()
        compose.onNodeWithText("指定日期").assertIsSelected()
        compose.onNodeWithText("日期待定").performClick()
        compose.onNodeWithText("指定日期").assertIsDisplayed().assertIsNotSelected()
        compose.onNodeWithText("指定日期").performClick()
        compose.onNodeWithText("确定日期").performClick()
        compose.onNodeWithText("指定日期").assertIsSelected()
        compose.onNodeWithText("2027-03-15").assertIsDisplayed()
        compose.onNodeWithTag("create-mode-SELF_DRIVE").assertIsSelected()
        compose.onNodeWithTag("create-submit").performClick()
        compose.waitUntil { repository.trip.value?.name == "日期旅行" }
        assertEquals(TravelMode.SELF_DRIVE, repository.trip.value!!.travelMode)
        assertEquals(LocalDate.of(2027, 3, 15), repository.trip.value!!.startDate)
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private class FakeImpacts : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = TripDeleteImpact(3, 2, 1, 4, 5)
        override suspend fun day(dayId: String) = DayDeleteImpact(2, 1, 2)
    }

    private data class MoveCall(val dayId: String, val targetIndex: Int)

    private class FakeTripRepository : TripRepository {
        private var nextId = 1
        val trip = MutableStateFlow<TripWithDays?>(null)
        private val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        val moveCalls = mutableListOf<MoveCall>()
        val deletedTrips = mutableListOf<String>()
        var deletedDays = 0
        var failCollectorsAfterDelete = 0
        fun seed(name: String, count: Int) { create(CreateTrip(name, count)) }
        private fun create(command: CreateTrip): String { val id=command.requestId ?: id(); trip.value=TripWithDays(id,command.name,command.startDate,command.travelMode,List(command.dayCount){TripDay(id(),it)}); publish(); return id }
        override fun observeTrips(): Flow<List<TripSummary>> = flow {
            trips.collect {
                if (deletedTrips.isNotEmpty() && failCollectorsAfterDelete > 0) {
                    failCollectorsAfterDelete--
                    throw IllegalStateException("db unavailable")
                }
                emit(it)
            }
        }
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun createTrip(command: CreateTrip) = create(command)
        override suspend fun renameTrip(tripId: String, name: String) {}
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) { trip.value=trip.value!!.copy(startDate=startDate); publish() }
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) { trip.value=trip.value!!.copy(travelMode=mode); publish() }
        override suspend fun insertDay(tripId:String,anchorDayId:String?,side:InsertSide):String { val d=trip.value!!.days.toMutableList();val id=id();val a=anchorDayId?.let{v->d.indexOfFirst{it.id==v}}?:d.size;d.add((a+if(side==InsertSide.AFTER)1 else 0).coerceIn(0,d.size),TripDay(id,0));setDays(d);return id }
        override suspend fun moveDay(tripId:String,dayId:String,targetIndex:Int){moveCalls += MoveCall(dayId,targetIndex);val d=trip.value!!.days.toMutableList();val day=d.removeAt(d.indexOfFirst{it.id==dayId});d.add(targetIndex,day);setDays(d)}
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion){deletedDays++;setDays(trip.value!!.days.filterNot{it.id==command.dayId})}
        override suspend fun deleteTrip(tripId:String){deletedTrips += tripId;trip.value=null;trips.value=emptyList()}
        private fun setDays(d:List<TripDay>){trip.value=trip.value!!.copy(days=d.mapIndexed{i,x->x.copy(index=i)});publish()}
        private fun publish(){trip.value?.let{trips.value=listOf(TripSummary(it.id,it.name,it.startDate,it.travelMode,it.days.size))}}
        private fun id()="id-${nextId++}"
    }
}
