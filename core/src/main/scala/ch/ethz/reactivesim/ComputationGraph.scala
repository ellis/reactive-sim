/*
Copyright 2013 Ellis Whitehead

This file is part of reactive-sim.

reactive-sim is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

reactive-sim is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with reactive-sim.  If not, see <http://www.gnu.org/licenses/>
*/
package ch.ethz.reactivesim

import scala.language.existentials
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scalaz._
import Scalaz._
import grizzled.slf4j.Logger
import scala.collection.mutable.ArrayBuffer
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scala.collection.SortedMap

trait Command
case class Command_ClearEntities(time: List[Int]) extends Command
case class Command_SetEntity(time: List[Int], id: String, entity: Object) extends Command
case class Command_AddCall(time: List[Int], call: Call) extends Command

trait GraphNode
case class CallNode(time: List[Int], call: Call) extends GraphNode {
	override def toString = time.mkString(".")
}
case class EntityNode(id: String) extends GraphNode {
	override def toString = id
}
case class SelectorNode(clazz: Class[_]) extends GraphNode {
	override def toString = clazz.toString+"*" //@"+time.map(_ + 1).mkString(".")
}

/**
 * Check status should be rechecked
 * InputMissing waiting for inputs to become available
 * InputReady inputs are all available
 * Next can be executed in the next step
 */
object CallStatus extends Enumeration {
	val Check, Waiting, Ready, Success, Error = Value
}

case class ComputationGraph(
	val g: Graph[GraphNode, UnDiEdge],
	val db: EntityBase,
	val timeToCall: SortedMap[List[Int], Call],
	val timeToIdToEntity: SortedMap[List[Int], Map[String, Object]],
	val timeToStatus: SortedMap[List[Int], CallStatus.Value],
	val timeToErrors: SortedMap[List[Int], List[String]],
	val timeToWarnings: SortedMap[List[Int], List[String]]
) {
	val isNextReady = timeToStatus.dropWhile(_._2 == CallStatus.Success).headOption match {
		case Some((time, status)) if status == CallStatus.Ready => true
		case _ => false
	}
	
	override def toString = {
		s"ComputationGraph(\n\t$g\n\t$db\n" +
			timeToCall.map(pair => {
				val time = pair._1
				val s = time.mkString(".")
				s"\t$s status: ${timeToStatus(time)}" +
					timeToIdToEntity(time).map(pair => s"\t\t${pair._1} = ${pair._2}").mkString("\n", "\n", "")
			}).mkString("\n") + ")"
	}
	
	def +(cmd: Command): ComputationGraph = {
		cmd match {
			case Command_ClearEntities(time) =>
				val db2 = db.clearEntities(time)
				val timeToIdToEntity2 = db2.getEntities
				val timeToStatus2 = calcCallStatus(g, db2, timeToCall, timeToIdToEntity2, timeToStatus)
				new ComputationGraph(
					g,
					db2,
					timeToCall,
					timeToIdToEntity2,
					timeToStatus2,
					timeToErrors,
					timeToWarnings
				)
			case Command_SetEntity(time, id, entity) =>
				val g2 = {
					if (ListIntOrdering.compare(time, List(0)) <= 0)
						g + EntityNode(id)
					else
						g + DiEdge[GraphNode](CallNode(time, timeToCall(time)), EntityNode(id))
				}
				val db2 = db.setEntity(time, id, entity)
				val timeToIdToEntity2 = db2.getEntities
				val timeToStatus1 = callStatusCheck(g2, time, id)
				val timeToStatus2 = calcCallStatus(g2, db2, timeToCall, timeToIdToEntity2, timeToStatus1)
				new ComputationGraph(
					g2,
					db2,
					timeToCall,
					timeToIdToEntity2,
					timeToStatus2,
					timeToErrors,
					timeToWarnings
				)
			case Command_AddCall(time, call) =>
				val g2 = g ++ addCall(time, call)
				val db2 = db.registerCall(time, None)
				val timeToCall2 = timeToCall + (time -> call)
				val timeToIdToEntity2 = db2.getEntities
				val timeToStatus2 = calcCallStatus(g2, db2, timeToCall2, timeToIdToEntity2, timeToStatus)
				new ComputationGraph(
					g2,
					db2,
					timeToCall2,
					timeToIdToEntity2,
					timeToStatus2,
					timeToErrors,
					timeToWarnings
				)
		}
	}
	
	def addCall(call: Call): ComputationGraph = {
		val time =
			if (timeToCall.isEmpty) List(1)
			else List(timeToCall.keys.max(ListIntOrdering).head + 1)
		this + Command_AddCall(time, call)
	}
	
	def setImmutableEntity(id: String, entity: Object): ComputationGraph = {
		this + Command_SetEntity(Nil, id, entity)
	}
	
	def setInitialState(id: String, entity: Object): ComputationGraph = {
		this + Command_SetEntity(List(0), id, entity)
	}

	/**
	 * When a call is added, its selectors are added to the selector/call graph
	 */
	private def addCall(time: List[Int], call: Call): Graph[GraphNode, UnDiEdge] = {
		val callNode = CallNode(time, call)
		// Create graph with either the new node, or the node with and incoming edge from its parent
		val g0 = {
			if (time.tail.isEmpty)
				Graph[GraphNode, UnDiEdge](callNode)
			else {
				val parent = CallNode(time.init, timeToCall(time.init))
				Graph[GraphNode, UnDiEdge](parent ~> callNode)
			}
		}
		// Add edges from entities and selectors to the call node
		call.selectors.flatMap(selector => processSelector(callNode, selector)).foldLeft(g0)(_ + _)
	}
	
	private def processSelector(callNode: CallNode, selector: Selector): Graph[GraphNode, UnDiEdge] = {
		selector match {
			case s: Selector_Entity =>
				Graph[GraphNode, UnDiEdge](EntityNode(s.id) ~> callNode)
			case s: Selector_List =>
				Graph.from(Nil, s.ids.map(id => DiEdge[GraphNode](EntityNode(id), callNode)))
			case s: Selector_All => // FIXME: add handling for Selector_ALL
				Graph[GraphNode, UnDiEdge](SelectorNode(s.clazz) ~> callNode)
		}
	}
	
	/**
	 * Change call status to CallStatus.Check for relevant nodes which depend on the entity with ID `id`.
	 * The relevant nodes are those after time `time` and before the entity is set again by another call. 
	 */
	private def callStatusCheck(
		g: Graph[GraphNode, UnDiEdge],
		time: List[Int],
		id: String
	): SortedMap[List[Int], CallStatus.Value] = {
		// List of times at which this entity is set after time `time`
		val timesSet = g.get(EntityNode(id)).diPredecessors.toOuterNodes.collect({case n: CallNode if ListIntOrdering.compare(n.time, time) > 0 => n.time})
		// Last time point we'll check
		val last = if (timesSet.isEmpty) List(Int.MaxValue) else timesSet.min(ListIntOrdering)
		// Call nodes which depend on the given entity
		val callNodes = g.get(EntityNode(id)).diSuccessors.toOuterNodes.collect({case n: CallNode => n})
		val checks = callNodes.filter(n => ListIntOrdering.compare(n.time, time) > 0 && ListIntOrdering.compare(n.time, last) <= 0)
		timeToStatus ++ checks.map(n => n.time -> CallStatus.Check)
	}
	
	private def calcCallStatus(
		g: Graph[GraphNode, UnDiEdge],
		db: EntityBase,
		timeToCall: SortedMap[List[Int], Call],
		timeToIdToEntity: SortedMap[List[Int], Map[String, Object]],
		timeToStatus: SortedMap[List[Int], CallStatus.Value]
	): SortedMap[List[Int], CallStatus.Value] = {
		// REFACTOR: map over timeToStatus, and make timeToStatus use a SortedMap
		SortedMap(timeToCall.keys.toSeq.sorted(ListIntOrdering).map(time => {
			val idToEntities = timeToIdToEntity(time)
			val call = timeToCall(time)
			val status = {
				val status0 = timeToStatus.getOrElse(time, CallStatus.Check)
				if (status0 == CallStatus.Check) {
					val ready =
						// Get entities required by the call node
						g.get(CallNode(time, call)).diPredecessors.toOuterNodes.collect({case n: EntityNode => n})
						// check whether the database has values for them all
						.forall(n => idToEntities.contains(n.id))
					
					if (ready) CallStatus.Ready else CallStatus.Waiting
				}
				else
					status0
			}
			time -> status
		}).toSeq : _*)(ListIntOrdering)
	}
	
	/**
	 * Step through computation by trying to execute the next call in time-order.
	 */
	def stepNext(): Option[ComputationGraph] = {
		timeToStatus.dropWhile(_._2 == CallStatus.Success).headOption match {
			case Some((time, status)) if status == CallStatus.Ready =>
				Some(stepOne(time))
			case _ => None
		}
	}
	
	/**
	 * Step through computation by trying to execute the first ready call available.
	 */
	def stepOne(): Option[ComputationGraph] = {
		timeToStatus.filter(_._2 == CallStatus.Ready).headOption match {
			case Some((time, _)) => Some(stepOne(time))
			case _ => None
		}
	}
	
	/**
	 * Step through computation by trying to execute all ready calls.
	 */
	def stepAllReady(): ComputationGraph = {
		// TODO: allow this to be run in parallel?
		timeToStatus.filter(_._2 == CallStatus.Ready).foldLeft(this) { (acc0, pair) =>
			val (time, status) = pair
			acc0.stepOne(time)
		}
	}
	
	def run(): ComputationGraph = {
		var cg = this
		do {
			cg = cg.stepAllReady()
		} while (!cg.timeToStatus.filter(_._2 == CallStatus.Ready).isEmpty)
		cg
	}
	
	private def stepOne(time: List[Int]): ComputationGraph = {
		val acc0 = this
		val call = acc0.timeToCall(time)
		val idToEntity = acc0.timeToIdToEntity(time)
		val inputs = call.selectors.map(_ match {
			case s: Selector_Entity => idToEntity(s.id)
			case s: Selector_List => s.ids.map(idToEntity)
			case s: Selector_All => Nil // FIXME: add handling for Selector_ALL
		})
		call.fn(inputs) match {
			case RsSuccess(results, warnings) =>
				// TODO: Add warnings
				val timeToStatus1 = acc0.timeToStatus + (time -> CallStatus.Success)
				// Check the next call now, if it was waiting
				val timeToStatus2 = timeToStatus1.keys.dropWhile(t => ListIntOrdering.compare(t, time) <= 0).headOption match {
					case Some(timeNext) if timeToStatus1(timeNext) == CallStatus.Waiting => timeToStatus1 + (timeNext -> CallStatus.Check)
					case _ => timeToStatus1
				}
				val timeToWarnings2 = warnings match {
					case Nil => timeToWarnings - time
					case _ => timeToWarnings + (time -> warnings)
				}
				// Set status of call to Success
				val acc1 = acc0.copy(timeToStatus = timeToStatus2, timeToWarnings = timeToWarnings2)
				// Add resulting commands
				val commands = Command_ClearEntities(time) :: processCallResults(time, results)
				val acc2 = commands.foldLeft(acc1) { (acc, cmd) => acc + cmd }
				acc2
			case RsError(errors, warnings) =>
				val timeToStatus2 = timeToStatus + (time -> CallStatus.Error)
				val timeToErrors2 = errors match {
					case Nil => timeToErrors - time
					case _ => timeToErrors + (time -> errors)
				}
				val timeToWarnings2 = warnings match {
					case Nil => timeToWarnings - time
					case _ => timeToWarnings + (time -> warnings)
				}
				// Set status of call to Success
				acc0.copy(timeToStatus = timeToStatus2, timeToErrors = timeToErrors2, timeToWarnings = timeToWarnings2)
		}
	}
	
	private def processCallResults(time: List[Int], results: List[CallResultItem]): List[Command] = {
		var childIndex = 0
		results.map(_ match {
			case CallResultItem_Entity(id, entity) =>
				Command_SetEntity(time, id, entity)
			case CallResultItem_Event(id, fn) =>
				val call = Call(
					fn = (args: List[Object]) => {
						val entity0 = args.head
						val entity1 = fn(entity0)
						RsSuccess(List(CallResultItem_Entity(id, entity1)))
					},
					selectors = Selector_Entity(id) :: Nil
				)
				childIndex += 1
				Command_AddCall(time ++ List(childIndex), call)
			case CallResultItem_Call(call) =>
				childIndex += 1
				Command_AddCall(time ++ List(childIndex), call)
		})
	}
}

object ComputationGraph {
	def apply(): ComputationGraph =
		new ComputationGraph(Graph(), new EntityBase(Map(), Map(), SortedMap()(ListIntOrdering)), SortedMap()(ListIntOrdering), SortedMap()(ListIntOrdering), SortedMap()(ListIntOrdering), SortedMap()(ListIntOrdering), SortedMap()(ListIntOrdering))
}
