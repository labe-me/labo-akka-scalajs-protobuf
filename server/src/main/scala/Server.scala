package me.labe.labo

import me.labe.labo.protocol._
import akka.actor._
import com.typesafe.config.ConfigFactory

object Server extends App {
  def config(role: String) = ConfigFactory.parseString(s"""
    | akka.loglevel="DEBUG"
    | akka.actor.provider="akka.remote.RemoteActorRefProvider"
    | akka.remote.enabled-transports=["akka.remote.netty.tcp"]
    | akka.remote.netty.tcp.hostname="127.0.0.1"
    | akka.remote.netty.tcp.port = ${if (role == "backend") 9990 else 0}
    | akka.actor.serializers {
    |   proto = "me.labe.labo.ProtobufSerializer"
    | }
    | akka.actor.serialization-bindings {
    |   "com.trueaccord.scalapb.GeneratedMessage" = proto
    |   "java.io.Serializable"                    = none
    | }
    | """.stripMargin)

  /* Start a remote akka system which will connect to a running backend and send some messages. */
  def startRemoteClient() = {
    println("Starting remote-client")
    val system = ActorSystem("remote-client", config("remote-client"))
    val path = ActorPath.fromString("akka.tcp://backend@127.0.0.1:9990/user/"+Communicator.name)
    val target = system.actorSelection(path)
    target ! Hello()
    target ! Hello(message = Some("Patatra"))
    target ! Empty()
    target ! Person(
      name = "Some one",
      id = 1200,
      email = None,
      phone = Seq(
        Person.PhoneNumber(number="0033123456", Some(Person.PhoneType.HOME))
      ),
      cdate = System.currentTimeMillis()
    )
    sys.ShutdownHookThread {
      system.terminate()
      Thread.sleep(2000)
    }
    Thread.sleep(3000)
  }

  /* Start web + akka remoting backend. */
  def startBackendServer() = {
    import akka.http.scaladsl.server.Directives._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.model.headers._
    import akka.http.scaladsl.model.headers.CacheDirectives._
    import akka.http.scaladsl.Http

    println("Starting backend")
    implicit val system = ActorSystem("backend", config("backend"))
    implicit val executor = system.dispatcher
    implicit val materializer = akka.stream.ActorMaterializer()
    // This is our akka remote joinable actor
    val communicator = system.actorOf(Communicator.props("backend"), name=Communicator.name)
    // This is our web routes
    val route =
      path("hello"){
        (post & entity(as[Array[Byte]])){ p =>
          val pp = Hello.parseFrom(p)
          communicator ! pp
          complete(pp.toString)
        }
      } ~
      path("person"){
        (post & entity(as[Array[Byte]])){ p =>
          val pp = Person.parseFrom(p)
          communicator ! pp
          complete(pp.toString)
        }
      } ~
      pathPrefix("js"){
        respondWithHeader(`Access-Control-Allow-Origin`.*){
          respondWithHeader(`Cache-Control`(`no-cache`,`no-store`,`must-revalidate`)){
            // Note: we expect to run this test server from sbt no need to complicate things,
            // just serve from the scalajs project.
            getFromDirectory("js/target/scala-2.11/")
          }
        }
      } ~
      pathEndOrSingleSlash {
        complete(
          HttpEntity(ContentTypes.`text/html(UTF-8)`, """<!doctype html>
<html>
  <head>
    <script type="text/javascript" src="/js/js-fastopt.js"></script>
  </head>
  <body>
    <script type="text/javascript">
     me.labe.labo.Client().main();
    </script>
  </body>
</html>
"""
          )
        )
      }
    Http().bindAndHandle(route , "127.0.0.1", 8888)
    sys.ShutdownHookThread {
      system.terminate()
      Thread.sleep(2000)
    }
  }

  (if (args.length == 0) "help" else args(0)) match {
    case "remote-client" => startRemoteClient()
    case "backend" => startBackendServer()
    case _ =>
      println("""Arguments:

  backend       -- start backend server with a akka remoting communicator and a webserver
  remote-client -- start remote akka client which will connect to the communicator

""")
  }
}

object Communicator {
  def name = "Communicator"
  def props(role: String) = Props(new Communicator(role))
}

class Communicator(role: String) extends Actor with ActorLogging {
  def receive = {
    case Empty() => log.debug("{} GOT empty...", role)
    case h: Hello => log.debug("{} GOT Hello {}", role, h.getMessage)
    case p: Person => log.debug("{} GOT Person {}", role, p)
    case x => log.error("{} mmm {}", role, x)
  }
}
