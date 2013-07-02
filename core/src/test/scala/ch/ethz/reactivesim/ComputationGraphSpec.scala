/*
Copyright 2013 Ellis Whitehead

This file is part of ComputationGraph.

ComputationGraph is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationGraph is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationGraph.  If not, see <http://www.gnu.org/licenses/>
*/
package ch.ethz.reactivesim

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen

class ComputationGraphSpec extends FunSpec with GivenWhenThen {
	
	val call0 = Call(
		fn = (args: List[Object]) => {
			RsSuccess(List(
				CallResultItem_Entity("output0", "Hello, World!")
			))
		},
		selectors = Nil
	)
	
	val call1 = Call(
		fn = (args: List[Object]) => {
			RsSuccess(List(
				CallResultItem_Entity("message", s"Hello, ${args.head}!")
			))
		},
		selectors = Selector_Entity("name") :: Nil
	)
	
	// toLowerCase
	val call2 = Call(
		fn = (args: List[Object]) => {
			RsSuccess(List(
				CallResultItem_Entity("text", args.head.toString.toLowerCase)
			))
		},
		selectors = Selector_Entity("text") :: Nil
	)
	
	// reverse
	val call3 = Call(
		fn = (args: List[Object]) => {
			RsSuccess(List(
				CallResultItem_Entity("text", args.head.toString.reverse)
			))
		},
		selectors = Selector_Entity("text") :: Nil
	)
	
	val t1 = List(1)
	val t2 = List(2)
	val t3 = List(3)
	
	describe("ComputationGraph") {
		it("call `call0` should be placed in the graph at time 1") {
			val x0 = ComputationGraph()
			val x1 = x0.addCall(call0)
			assert(x1.g.nodes.toNodeInSet === Set(CallNode(t1, call0)))
		}
		
		it("call `call1` should be ready once its input is available") {
			val x0 = ComputationGraph()
			val x1 = x0.addCall(call1)
			//println(x1.g)
			val cn1 = CallNode(t1, call1)
			assert(x1.g.nodes.toNodeInSet ===
				Set(
					cn1,
					EntityNode("name")
				)
			)
			assert(x1.timeToStatus(t1) === CallStatus.Waiting)
			
			// Set the input entity value to "World"
			val x2 = x1.setImmutableEntity("name", "World")
			// Graph nodes should be unchanged
			assert(x2.g.nodes.toNodeInSet ===
				Set(
					cn1,
					EntityNode("name")
				)
			)
			// The call should now be ready
			assert(x2.timeToStatus(t1) === CallStatus.Ready)
			
			val x3 = x2.stepNext().get
			//println(x3.g)
			// Graph nodes be now also contain `message`
			assert(x3.g.nodes.toNodeInSet ===
				Set(
					cn1,
					EntityNode("name"),
					EntityNode("message")
				)
			)
			assert(x3.db.getEntity(t1, "message") === None)
			assert(x3.db.getEntity(List(2), "message") === Some("Hello, World!"))
			// The call should have succeeded
			assert(x3.timeToStatus(t1) === CallStatus.Success)
		}
	}
	
	describe("ComputationGraph to process text by two functions in sequence") {
		it("should transform 'AbCdE' to 'edcba', then 'REVERSE' to 'esrever'") {
			val x1 = ComputationGraph().addCall(call2).addCall(call3)
			//println(x1.g)
			val cn1 = CallNode(t1, call2)
			val cn2 = CallNode(t2, call3)
			assert(x1.g.nodes.toNodeInSet ===
				Set(
					cn1,
					cn2,
					EntityNode("text")
				)
			)
			assert(x1.timeToStatus(t1) === CallStatus.Waiting)
			assert(x1.timeToStatus(t2) === CallStatus.Waiting)
			
			val x2 = x1.setInitialState("text", "AbCdE")
			assert(x2.timeToStatus(t1) === CallStatus.Ready)
			assert(x2.timeToStatus(t2) === CallStatus.Waiting)
			assert(x2.timeToIdToEntity(t1).get("text") === Some("AbCdE"))
			assert(x2.timeToIdToEntity(t2).get("text") === None)
			assert(x2.timeToIdToEntity(t3).get("text") === None)
			
			val x3 = x2.stepNext().get
			//info(x3.toString)
			assert(x3.timeToStatus(t1) === CallStatus.Success)
			assert(x3.timeToStatus(t2) === CallStatus.Ready)
			assert(x3.timeToIdToEntity(t1).get("text") === Some("AbCdE"))
			assert(x3.timeToIdToEntity(t2).get("text") === Some("abcde"))
			assert(x3.timeToIdToEntity(t3).get("text") === None)
			
			val x4 = x3.stepNext().get
			//info(x4.toString)
			assert(x4.timeToStatus(t1) === CallStatus.Success)
			assert(x4.timeToStatus(t2) === CallStatus.Success)
			assert(x4.timeToIdToEntity(t1).get("text") === Some("AbCdE"))
			assert(x4.timeToIdToEntity(t2).get("text") === Some("abcde"))
			assert(x4.timeToIdToEntity(t3).get("text") === Some("edcba"))
			
			val x5 = x4.setInitialState("text", "REVERSE")
			assert(x5.timeToStatus(t1) === CallStatus.Ready)
			assert(x5.timeToStatus(t2) === CallStatus.Success)
			assert(x5.timeToIdToEntity(t1).get("text") === Some("REVERSE"))
			assert(x5.timeToIdToEntity(t2).get("text") === Some("abcde"))
			assert(x5.timeToIdToEntity(t3).get("text") === Some("edcba"))
			
			val x6 = x5.stepNext().get
			assert(x6.timeToStatus(t1) === CallStatus.Success)
			assert(x6.timeToStatus(t2) === CallStatus.Ready)
			assert(x6.timeToIdToEntity(t1).get("text") === Some("REVERSE"))
			assert(x6.timeToIdToEntity(t2).get("text") === Some("reverse"))
			assert(x6.timeToIdToEntity(t3).get("text") === Some("edcba"))
			
			val x7 = x6.stepNext().get
			assert(x7.timeToStatus(t1) === CallStatus.Success)
			assert(x7.timeToStatus(t2) === CallStatus.Success)
			assert(x7.timeToIdToEntity(t1).get("text") === Some("REVERSE"))
			assert(x7.timeToIdToEntity(t2).get("text") === Some("reverse"))
			assert(x7.timeToIdToEntity(t3).get("text") === Some("esrever"))
		}
	}
}