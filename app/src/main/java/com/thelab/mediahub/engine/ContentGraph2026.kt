package com.thelab.mediahub.engine

import com.thelab.mediahub.data.MediaEntity

data class GraphNode(val id: String, val label: String, val category: String)
data class GraphEdge(val sourceId: String, val targetId: String, val relationship: String)

class ContentGraph2026 {

    private val nodes = mutableMapOf<String, GraphNode>()
    private val edges = mutableListOf<GraphEdge>()

    fun buildGraph(items: List<MediaEntity>) {
        nodes.clear()
        edges.clear()

        for (item in items) {
            val node = GraphNode(item.uriString, item.fileName, item.category.name)
            nodes[item.uriString] = node

            items.filter { it.parentFolder == item.parentFolder && it.uriString != item.uriString }.forEach { related ->
                edges.add(GraphEdge(item.uriString, related.uriString, "SAME_ORIGIN"))
            }
        }
    }

    fun getRelated2026Finds(uriString: String): List<GraphNode> {
        val connectedTargetIds = edges.filter { it.sourceId == uriString }.map { it.targetId }
        return nodes.filterKeys { it in connectedTargetIds }.values.toList()
    }
}
