package lila

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import ornicar.scalalib.ScalalibExtensions

package object ws extends ScalalibExtensions:

  type Emit[A] = Function[A, Unit]

  type ClientSystem   = ActorSystem[Clients.Control]
  type ClientBehavior = Behavior[ipc.ClientMsg]
  type Client         = ActorRef[ipc.ClientMsg]
  type ClientEmit     = Emit[ipc.ClientIn]

  type ~[+A, +B] = Tuple2[A, B]
  object ~ :
    def apply[A, B](x: A, y: B)                              = Tuple2(x, y)
    def unapply[A, B](x: Tuple2[A, B]): Option[Tuple2[A, B]] = Some(x)
