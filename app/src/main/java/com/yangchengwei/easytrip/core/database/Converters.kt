package com.yangchengwei.easytrip.core.database
import androidx.room.TypeConverter
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import java.time.*
class Converters {
 @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
 @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)
 @TypeConverter fun localTimeToString(v: LocalTime?): String? = v?.toString()
 @TypeConverter fun stringToLocalTime(v: String?): LocalTime? = v?.let(LocalTime::parse)
 @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
 @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)
 @TypeConverter fun timeModeToString(v: TimeMode): String = v.name
 @TypeConverter fun stringToTimeMode(v: String): TimeMode = TimeMode.valueOf(v)
 @TypeConverter fun travelModeToString(v: TravelMode): String = v.name
 @TypeConverter fun stringToTravelMode(v: String): TravelMode = TravelMode.valueOf(v)
 @TypeConverter fun transportModeToString(v: TransportMode?): String? = v?.name
 @TypeConverter fun stringToTransportMode(v: String?): TransportMode? = v?.let(TransportMode::valueOf)
 @TypeConverter fun routeStatusToString(v: RouteStatus): String = v.name
 @TypeConverter fun stringToRouteStatus(v: String): RouteStatus = RouteStatus.valueOf(v)
 @TypeConverter fun routeErrorKindToString(v: RouteErrorKind?): String? = v?.name
 @TypeConverter fun stringToRouteErrorKind(v: String?): RouteErrorKind? = v?.let(RouteErrorKind::valueOf)
}
