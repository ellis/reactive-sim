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

class ReactiveSim {
	private var cg = ComputationGraph()
	
	def graph = cg
	
	/**
	 * Add a top-level call to the computation graph
	 */
	def addCall(call: Call) {
		cg = cg.addCall(call)
	}
	
	/**
	 * Set the initial value for a given id
	 */
	def setInitialState(id: String, entity: Object) {
		cg = cg.setInitialState(id, entity)
	}
	
	/**
	 * Run the computation as far as it can go
	 */
	def run() {
		cg = cg.run()
	}
	
	/**
	 * Get list of errors and warnings from the computation
	 */
	def messages(): List[String] = {
		val l: List[(List[Int], String)] =
			cg.timeToErrors.toList.flatMap(pair => pair._2.map(s => pair._1 -> s"ERROR: Call ${pair._1.mkString(".")}: $s")) ++
			cg.timeToWarnings.toList.flatMap(pair => pair._2.map(s => pair._1 -> s"WARNING: Call ${pair._1.mkString(".")}: $s"))
		l.sortBy(_._1)(ListIntOrdering).map(_._2)
	}
}