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

import scala.reflect.runtime.universe.Type

case class Call(fn: List[Object] => RsResult[List[CallResultItem]], selectors: List[Selector])

sealed trait CallResultItem
case class CallResultItem_Entity(id: String, entity: Object) extends CallResultItem
case class CallResultItem_Event(id: String, fn: Object => Object) extends CallResultItem
case class CallResultItem_Call(call: Call) extends CallResultItem
