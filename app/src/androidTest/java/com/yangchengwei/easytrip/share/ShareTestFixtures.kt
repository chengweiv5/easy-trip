package com.yangchengwei.easytrip.share

import com.yangchengwei.easytrip.core.model.GeoPoint
import java.time.LocalDate
import java.time.LocalTime

internal fun shareFixture(): ShareTrip {
    val points = listOf(GeoPoint(30.248,120.149),GeoPoint(30.238,120.153),GeoPoint(30.231,120.143))
    return ShareTrip("share-test", "杭州 · 湖畔慢游", (0..2).map { day ->
        ShareDay("d$day",day,LocalDate.of(2026,4,12).plusDays(day.toLong()),
            listOf("西湖天地", "柳浪闻莺", "雷峰塔").mapIndexed { i,name ->
                ShareStop("$day-$i",i+1,name,points[i],LocalTime.of(9+i*2,30),60,
                    if(i==0)"沿湖散步，记得带水。\n从南侧入口进入。" else "预留休息时间，按体力调整。",
                    if(i<2)ShareLeg("步行 · 15 分钟 · 1 公里","雨天可以改乘公交",listOf(points[i],points[i+1]),true) else null)
            })
    })
}
