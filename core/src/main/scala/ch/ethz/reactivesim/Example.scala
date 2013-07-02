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

case class Container(id: String)
case class ContainerState(volume: Double)

trait RobotCommand
case class Aspirate(volume: Double, container: Container) extends RobotCommand
case class Dispense(volume: Double, container: Container) extends RobotCommand
case class Measure(container: Container) extends RobotCommand
case class AlertUser(message: String) extends RobotCommand

trait RobotEvent
case class AspriateEvent()
case class DispenseEvent()
case class MeasureEvent()

class Example {
	val A = Container("A")
	val B = Container("B")
	
	def exec(command: RobotCommand): CallResultItem_Call = {
		val call = command match {
			case Aspirate(volume, container) =>
				val selectors = List[Selector](Selector_Entity(container.id))
				val fn = (inputs: List[Object]) => {
					val state = inputs(0).asInstanceOf[ContainerState]
					RsSuccess(List[CallResultItem](CallResultItem_Entity(container.id, state.copy(volume = state.volume - volume))))
				}
				Call(fn, selectors)
			case Dispense(volume, container) =>
				val selectors = List[Selector](Selector_Entity(container.id))
				val fn = (inputs: List[Object]) => {
					val state = inputs(0).asInstanceOf[ContainerState]
					RsSuccess(List[CallResultItem](CallResultItem_Entity(container.id, state.copy(volume = state.volume + volume))))
				}
				Call(fn, selectors)
			case Measure(container) =>
				val selectors = List[Selector](Selector_Entity(container.id))
				val fn = (inputs: List[Object]) => {
					val state = inputs(0).asInstanceOf[ContainerState]
					RsSuccess(List[CallResultItem](CallResultItem_Entity(container.id, state)))
				}
				Call(fn, selectors)
		}
		CallResultItem_Call(call)
	}
	
	def x(): List[CallResultItem] = x(0, 10)
	
	def x(index: Int, volume: Double): List[CallResultItem] = {
		exec(Aspirate(volume, A)) :: exec(Dispense(volume, B)) :: exec(Measure(B)) :: check(index, 10) :: Nil
	}
	
	def check(index: Int, target: Double): CallResultItem = {
		val selectors = List[Selector](Selector_Entity(s"measurement$index"))
		val fn = (inputs: List[Object]) => {
			val measurement = inputs(0).asInstanceOf[java.lang.Double]
			val f = measurement / target
			if (f > 1.1)
				RsSuccess(exec(AlertUser("Threshold exceeded")) :: Nil)
			else if (f < 0.9)
				RsSuccess(x(index + 1, target - measurement))
			else
				RsSuccess(Nil)
		}
		CallResultItem_Call(Call(fn, selectors))
	}
	
	
}

object Example {
	def main(args: Array[String]) {
		val e = new Example
		val call = Call(
			fn = (inputs: List[Object]) => {
				RsSuccess(e.x())
			},
			selectors = Nil
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
