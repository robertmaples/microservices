package shopping.cart

import com.typesafe.config.ConfigFactory
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import akka.cluster.ddata.VersionVector

object ShoppingCartSpec {
  val config = ConfigFactory
    .parseString("""
		akka.actor.serialization-bindings {
			"shopping.cart.CborSerializable" = jackson-cbor
		}
	""")
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class ShoppingCartSpec
    extends ScalaTestWithActorTestKit(ShoppingCartSpec.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {
  private val cartId = "testCart"
  private val eventSourcedTestKit = EventSourcedBehaviorTestKit[
    ShoppingCart.Command,
    ShoppingCart.Event,
    ShoppingCart.State](system, ShoppingCart(cartId))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The Shopping Cart" should {
    "add item" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          replyTo => ShoppingCart.AddItem("foo", 42, replyTo))
      result1.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false)))
      result1.event should ===(ShoppingCart.ItemAdded(cartId, "foo", 42))
    }

    "reject already added item" in {
      eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
        replyTo => ShoppingCart.AddItem("foo", 42, replyTo))
      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          replyTo => ShoppingCart.AddItem("foo", 42, replyTo))

      result2.reply.isError should ===(true)
    }

    "checkout" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, _))
      result1.reply.isSuccess should ===(true)
      val result2 = eventSourcedTestKit
        .runCommand[StatusReply[ShoppingCart.Summary]](ShoppingCart.Checkout(_))
      result2.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = true)))
      result2.event.asInstanceOf[ShoppingCart.CheckedOut].cartId should ===(
        cartId)

      val result3 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("bar", 13, _))
      result3.reply.isError should ===(true)
    }

    "get" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, _))
      result1.reply.isSuccess should ===(true)

      val result2 = eventSourcedTestKit.runCommand[ShoppingCart.Summary](
        ShoppingCart.Get(_))
      result2.reply should ===(
        ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false))
    }
  }
}
