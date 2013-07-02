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

import scala.language.implicitConversions
import scala.reflect.Manifest

sealed trait Lookup[A] {
	val clazz: Class[_]
	val selector: Selector
}
case class Lookup_Entity[A : Manifest](id: String) extends Lookup[A] {
	val clazz = manifest[A].erasure
	val selector = Selector_Entity(id)
}
case class Lookup_List[A : Manifest](ids: Seq[String], isOptional: Boolean = false) extends Lookup[List[A]] {
	val clazz = manifest[List[A]].erasure
	val selector = Selector_List(ids)
}
case class Lookup_All[A : Manifest]() extends Lookup[List[A]] {
	val clazz = manifest[List[A]].erasure
	val selector = Selector_All(manifest[A].erasure)
}

trait Dsl {
	
	/**
	 * Construct a call with no inputs
	 */
	def select()(fn: Unit => RsResult[List[CallResultItem]]): Call = {
		Call(
			fn = (inputs: List[Object]) => {
				fn()
			},
			selectors = List[Selector]()
		)
	}
	
	/**
	 * Construct a call with 1 input
	 */
	def select[A](a: Lookup[A])(fn: (A) => RsResult[List[CallResultItem]]): Call = {
		Call(
			fn = (inputs: List[Object]) => {
				val a1 = inputs(0).asInstanceOf[A]
				fn(a1)
			},
			selectors = List[Selector](a.selector)
		)
	}
	
	/**
	 * Construct a call with 2 inputs
	 */
	def select[A, B](a: Lookup[A], b: Lookup[B])(fn: (A, B) => RsResult[List[CallResultItem]]): Call = {
		Call(
			fn = (inputs: List[Object]) => {
				val a1 = inputs(0).asInstanceOf[A]
				val b1 = inputs(1).asInstanceOf[B]
				fn(a1, b1)
			},
			selectors = List[Selector](a.selector, b.selector)
		)
	}
	
	def as[A : Manifest](id: String): Lookup_Entity[A] = Lookup_Entity[A](id)
	
	def output(items: CallResultItem*): List[CallResultItem] = items.toList
	
	implicit def pairToResultEntity(pair: (String, Object)) = CallResultItem_Entity(pair._1, pair._2)
	implicit def callToResult(call: Call) = CallResultItem_Call(call)
	
}