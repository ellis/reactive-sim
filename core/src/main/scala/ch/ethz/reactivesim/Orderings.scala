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

object ListIntOrdering extends Ordering[List[Int]] {
	def compare(a: List[Int], b: List[Int]): Int = {
		(a, b) match {
			case (Nil, Nil) => 0
			case (Nil, _) => -1
			case (_, Nil) => 1
			case (a1 :: rest1, a2 :: rest2) =>
				if (a1 != a2) a1 - a2
				else compare(rest1, rest2)
		}
	}
}

object ListStringOrdering extends Ordering[List[String]] {
	def compare(a: List[String], b: List[String]): Int = {
		(a, b) match {
			case (Nil, Nil) => 0
			case (Nil, _) => -1
			case (_, Nil) => 1
			case (a1 :: rest1, a2 :: rest2) =>
				val n = a1.compare(a2)
				if (n != 0) n
				else compare(rest1, rest2)
		}
	}
}
