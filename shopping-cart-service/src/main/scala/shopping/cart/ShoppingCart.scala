package shopping.cart

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

object ShoppingCart {

  /**
   * This interface defines all the commands (messages) that the ShoppingCart actor supports.
   */
  sealed trait Command extends CborSerializable

  /**
   * A command to add an item to the cart.
   *
   * It replies with `StatusReply[Summary]`, which is sent back to the caller when
   * all the events emitted by this command are successfully persisted.
   */
  final case class AddItem(
      itemId: String,
      quantity: Int,
      replyTo: ActorRef[StatusReply[Summary]])
      extends Command

  /**
   * Summary of the shopping cart state, used in reply messages.
   */
  final case class Summary(items: Map[String, Int]) extends CborSerializable

  /**
   * This interface defines all the events that the ShoppingCart supports.
   */
  sealed trait Event extends CborSerializable {
    def cartId: String
  }

  final case class ItemAdded(cartId: String, itemId: String, quantity: Int)
      extends Event

  final case class State(items: Map[String, Int]) extends CborSerializable {

    def hasItem(itemId: String): Boolean = items.contains(itemId)

    def isEmpty: Boolean = items.isEmpty

    def updateItem(itemId: String, quantity: Int): State = {
      quantity match {
        case 0 => copy(items = items - itemId)
        case _ => copy(items = items + (itemId -> quantity))
      }
    }
  }
  object State {
    val empty = State(items = Map.empty)
  }
}
