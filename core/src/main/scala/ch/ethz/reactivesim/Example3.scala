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

import scala.reflect.Manifest
/*
class Example3 {
	val A = Container("A")
	val B = Container("B")
	
	def input()(fn: Unit => List[CallResultItem]): Call = {
		Call(
			fn = (inputs: List[Object]) => {
				fn()
			},
			args = List[Selector]()
		)
	}
	
	def input[A](a: Lookup[A])(fn: (A) => List[CallResultItem]): Call = {
		Call(
			fn = (inputs: List[Object]) => {
				val a1 = inputs(0).asInstanceOf[A]
				fn(a1)
			},
			args = List[Selector](a.selector)
		)
	}
	
	def as[A : Manifest](id: String): Lookup_Entity[A] = Lookup_Entity[A](id)
	
	def output(items: CallResultItem*): List[CallResultItem] = items.toList
	
	implicit def pairToResultEntity(pair: (String, Object)) = CallResultItem_Entity(pair._1, pair._2)
	implicit def callToResult(call: Call) = CallResultItem_Call(call)
	
	def exec(command: RobotCommand): Call = {
		command match {
			case Aspirate(volume, container) =>
				input (as[ContainerState](container.id)) {
					(state) =>
					output(container.id -> state.copy(volume = state.volume - volume))
				}
			case Dispense(volume, container) =>
				input (as[ContainerState](container.id)) {
					(state) =>
					output(container.id -> state.copy(volume = state.volume + volume))
				}
			case Measure(container) =>
				val selectors = List[Selector](Selector_Entity(container.id))
				val fn = (inputs: List[Object]) => {
					val state = inputs(0).asInstanceOf[ContainerState]
					List[CallResultItem](CallResultItem_Entity(container.id, state))
				}
				Call(fn, selectors)
		}
	}
	
	def x(): List[CallResultItem] = x(0, 10)
	
	def x(index: Int, volume: Double): List[CallResultItem] = {
		for {
			_ <- exec(Aspirate(volume, A))
			_ <- exec(Dispense(volume, B))
			_ <- exec(Measure(B))
			check(index, 10)
		} yield 
		output(
			exec(Aspirate(volume, A)),
			exec(Dispense(volume, B)),
			exec(Measure(B)),
			check(index, 10)
		)
	}
	
	def check(index: Int, target: Double): Call = {
		input(as[java.lang.Double](s"measurement$index")) {
			(measurement) =>
			val f = measurement / target
			if (f > 1.1)
				output(exec(AlertUser("Threshold exceeded")))
			else if (f < 0.9)
				x(index + 1, target - measurement)
			else
				Nil
		}
	}
	
}

object Example3 {
	def main(args: Array[String]) {
		val e = new Example3
		val call = Call(
			fn = (inputs: List[Object]) => {
				e.x()
			},
			args = Nil
		)
		var cg = ComputationGraph()
			.addCall(call)
			.setInitialState(e.A.id, ContainerState(20))
			.setInitialState(e.B.id, ContainerState(0))
		println(cg)
		println()
		
		cg = cg.run()
		println("cg:")
		println(cg)
		println()
		
		cg = cg.setInitialState("measurement0", 8.0: java.lang.Double)
		
		cg = cg.run()
		println("cg:")
		println(cg)
		println()
		
		cg = cg.setInitialState("measurement1", 10.0: java.lang.Double)
		
		cg = cg.run()
		println("cg:")
		println(cg)
		println()
	}
}
*/