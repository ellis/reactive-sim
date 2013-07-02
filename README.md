============
reactive-sim
============

Compiling
---------

Code should be compiled with `sbt` in the `core` directory.

Structure
---------

There are four important concepts:

* Data (immutable, state)
* Call
* Selector
* Event

There are three types of data: immutable entities, initial states, and non-initial states.
All three are represented by a 4-tuple (id, type, time, value).

A call is composed of a function and a list of input selectors.

There are three types of selectors: single data item, list of data items, all items of a given type.
The first two can be optional, meaning that the function will be called even if the items are unavailable.

Functions can return data, events, and calls.  Also constraints.

Entity data structure
---------------------

Immutable entities are available to all calls.
In contrast, the availability of mutable entities is more complex.
Entities which have been set at time 1 (the "initial state" of the system) are available to call 1.
Since the first call might change any of the mutable entities, they are all masked from the call 2 until call 1 has completed.
If the programmer specifies constraints for which entities call 1 can change, however, then all other mutable entities are available to call 2
even before call 1 completes.

Missing data
------------

When data is missing, defaults can be supplied by special functions.

Use cases
---------

This approach is good for two use-cases in particular:
* Letting a user step through, trouble-shoot, or visualize a computation, or
* to explore the impact of parameter changes on a computation.

*Computations*: Generally these would be ones that the user constructs using
your program.  It could be a simulation, a graphical programming language, or
parsed code.

Examples
--------

Need to complete.

Documentation
-------------

Need to complete.

Related topics
--------------

* Bret Victor - Learnable Programming (http://worrydream.com/LearnableProgramming/)
* AngularJS
* Functional Reactive Programming (FRP)
* Event sources (<http://martinfowler.com/eaaDev/EventSourcing.html>)
* CQRS (<http://martinfowler.com/bliki/CQRS.html>)
* JavaFX/ScalaFX
* Rx, a reactive framework from Microsoft
