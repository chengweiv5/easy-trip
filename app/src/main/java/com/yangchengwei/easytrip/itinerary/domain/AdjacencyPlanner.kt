package com.yangchengwei.easytrip.itinerary.domain

data class Edge(val fromItemId: String, val toItemId: String)

data class AdjacencyDiff(
    val deleted: Set<Edge>,
    val created: Set<Edge>,
)

fun adjacencyDiff(old: List<String>, new: List<String>): AdjacencyDiff {
    val oldEdges = old.zipWithNext(::Edge).toSet()
    val newEdges = new.zipWithNext(::Edge).toSet()
    return AdjacencyDiff(
        deleted = oldEdges - newEdges,
        created = newEdges - oldEdges,
    )
}
