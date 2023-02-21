package ChatBox

//import com.typesafe.scalalogging.Logger

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket, SocketException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MultiThreadServer{
  //val logger = Logger("Root")

  def main(args: Array[String]): Unit = {

    var serverKeyInput: String = null
    try {
      val userList = scala.collection.mutable.ListBuffer[ClientHandler]()
      var keyInput = new BufferedReader(new InputStreamReader(System.in))
      val serverSocket = new ServerSocket(9000)
     // logger.debug("Server Started")
      println("Server Started")
      while (true) {
        val clientSocket = serverSocket.accept()
        var userName: String = null
        Future{
          serverKeyInput = keyInput.readLine()
          if(serverKeyInput.equalsIgnoreCase("quit")){
           // logger.debug("Server Quit")
            println("Server Quit")
            serverSocket.close()
          }
        }
        Future {
          val input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
          val output = new PrintWriter(clientSocket.getOutputStream, true)
          output.println("Welcome to the chat room!")
          output.println("Enter Your Name")
          userName = input.readLine()
         // logger.debug(s"$userName Connected.")
          println(s"$userName Connected.")
          val clientHandler = new ClientHandler(clientSocket, userList, userName)
          userList += clientHandler
          clientHandler.setName(userName)
          clientHandler.start()
        }
      }
    }catch{
      case e : SocketException if e.getMessage=="socket closed" => {
        println("Server Socket Closed")
       // logger.debug("Server Socket Closed")
      }
      case e : Exception => println(s"Error While Starting Server ${e.getMessage}")
    }
  }

}

class ServerMethods{
  def connectedUser(userList: scala.collection.mutable.ListBuffer[ClientHandler]): Unit = {
    val UserPresent = userList.map(_.getName).mkString(", ")
    userList.map(_.outputFromServer.println(UserPresent))
  }

  def broadcast(message: String, sender: ClientHandler, userList: scala.collection.mutable.ListBuffer[ClientHandler]): Unit = {
    //logger.info("Inside BroadCast")
    try {
      for (user <- userList) {
        if (message.contains("@" + user.getName) && user != sender) {
          val sendMsg = message.replace("@" + user.getName, "")
          user.outputFromServer.println(sendMsg)
        }
        else if (user != sender && !message.contains("@")) {
          user.outputFromServer.println(message)
        }
      }
    } catch {
      case e: IOException => println(s"IOException Catch ${e.getMessage}")
    }
  }
}

class ClientHandler(clientSocket: Socket, userList: scala.collection.mutable.ListBuffer[ClientHandler],userName: String) extends Thread {
  //MultiThreadServer.logger.debug("Inside ClientHandler")
  val serverMethod = new ServerMethods
  val inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
  val outputFromServer = new PrintWriter(clientSocket.getOutputStream, true)

  override def run(): Unit = {
    if (userList.nonEmpty) {
      serverMethod.connectedUser(userList)
    }
    try {
      while (true) {
        val message = inputFromClient.readLine()
        if (message == "quit" || message == null) {
          try {
            throw new SocketException("Connection closed by client")
          } catch {
            case e: SocketException => {
              //MultiThreadServer.logger.debug(s"$userName Disconnected")
              println(s"$userName Disconnected")
              clientSocket.close()
              //MultiThreadServer.logger.debug(s"Is client Socket Closed ${clientSocket.isClosed}")
              println("sIs client Socket Closed ${clientSocket.isClosed}")
              println(this.clientSocket.isClosed)
              userList -= this
              println(s"User Removed from user list ${userList}")
              //MultiThreadServer.logger.debug(s"User Removed from user list ${userList}")
            }
            //case e: IOException => println("Stream Closed")
          }
        } else {
          serverMethod.broadcast(s"$userName: $message", this, userList)
        }
      }
    }catch{
      case e : IOException => println(s"$userName Stream Closed")
    }
  }
}
