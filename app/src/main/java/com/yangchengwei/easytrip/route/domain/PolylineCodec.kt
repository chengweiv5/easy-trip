package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.core.model.GeoPoint
object PolylineCodec { private const val PREFIX="v1|"; fun encode(points:List<GeoPoint>)=PREFIX+points.joinToString(";"){"${it.latitude},${it.longitude}"}; fun decode(value:String):Result<List<GeoPoint>> = runCatching { require(value.startsWith(PREFIX)); value.removePrefix(PREFIX).takeIf(String::isNotEmpty)?.split(';')?.map { token -> val parts=token.split(','); require(parts.size==2); GeoPoint(parts[0].toDouble(),parts[1].toDouble()) }.orEmpty() } }
