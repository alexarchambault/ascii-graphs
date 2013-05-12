package com.github.mdr.ascii.graph

import scala.PartialFunction.cond
import com.github.mdr.ascii.util.Utils
import com.github.mdr.ascii.util.Utils._
import com.github.mdr.ascii.layout.Layouter
import com.github.mdr.ascii.parser.Diagram
import com.github.mdr.ascii.parser.Box
import com.github.mdr.ascii.layout.GraphLayout

object Graph {

  def fromDiagram(s: String): Graph[String] = fromDiagram(Diagram(s))

  def fromDiagram(diagram: Diagram): Graph[String] = {
    val boxToVertexMap: Map[Box, String] = makeMap(diagram.childBoxes, _.text)

    val vertices = boxToVertexMap.values.toSet
    val edges =
      for {
        edge ← diagram.allEdges
        vertex1 ← boxToVertexMap.get(edge.box1)
        vertex2 ← boxToVertexMap.get(edge.box2)
      } yield {
        if (edge.hasArrow2)
          vertex1 -> vertex2
        else
          vertex2 -> vertex1
      }
    Graph(vertices, edges)
  }

}

case class Graph[V](vertices: Set[V], edges: List[(V, V)]) {

  val outMap: Map[V, List[V]] = edges.groupBy(_._1).map { case (k, vs) ⇒ (k, vs.map(_._2)) }

  val inMap: Map[V, List[V]] = edges.groupBy(_._2).map { case (k, vs) ⇒ (k, vs.map(_._1)) }

  require(outMap.keys.forall(vertices.contains))

  require(inMap.keys.forall(vertices.contains))

  def isEmpty = vertices.isEmpty

  def inEdges(v: V): List[(V, V)] = edges.filter(_._2 == v)

  def outEdges(v: V): List[(V, V)] = edges.filter(_._1 == v)

  def inVertices(v: V): List[V] = inMap.getOrElse(v, Nil)

  def outVertices(v: V): List[V] = outMap.getOrElse(v, Nil)

  def outDegree(v: V): Int = outVertices(v).size

  def inDegree(v: V): Int = inVertices(v).size

  def sources: List[V] = vertices.toList.filter(inDegree(_) == 0)

  def sinks: List[V] = vertices.toList.filter(outDegree(_) == 0)

  def removeEdge(edge: (V, V)): Graph[V] = copy(edges = Utils.removeFirst(edges, edge))

  def removeVertex(v: V): Graph[V] =
    Graph(vertices.filterNot(_ == v), edges.filterNot { case (v1, v2) ⇒ v1 == v || v2 == v })

  def map[U](f: V ⇒ U): Graph[U] =
    Graph(vertices.map(f), edges.map { case (v1, v2) ⇒ (f(v1), f(v2)) })

  override lazy val hashCode = vertices.## + edges.##

  override def equals(obj: Any): Boolean = cond(obj) {
    case other: Graph[V] ⇒
      multisetCompare(vertices.toList, other.vertices.toList) &&
        multisetCompare(edges, other.edges)
  }

  override def toString = "\n" + GraphLayout.renderGraph(this)

}