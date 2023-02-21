package ChatBox

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.{Socket, SocketException}

object MultiThreadClientServer {
  def main(args: Array[String]): Unit = {
    val socket = new Socket("localhost", 9000)
    val inputFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val outputFromClient = new PrintWriter(socket.getOutputStream, true)
    new Thread(new InputHandler(outputFromClient)).start()
    try {

      while (true) {
        val message = inputFromServer.readLine()
        if (message == null || message.equalsIgnoreCase("quit")) {
          //println("Client Wants to Quit")
          throw new SocketException("Server Connection Closed")
        }
        println(message)
      }
    } catch {
      case e: SocketException => {
        inputFromServer.close()
        outputFromClient.close()
        socket.close()
        println(s"Disconnected From Server ${e.getMessage}")
      }
      case e: Exception => println(s"Exception ${e.getMessage}")
    }
  }

  class InputHandler(outputFromClient: PrintWriter) extends Thread {
    override def run(): Unit = {
      val inputMsg = new BufferedReader(new InputStreamReader(System.in))
      try {
        while (true) {
          println("<<>>")
          val message = inputMsg.readLine()
          try {
            if (message == null && message.equalsIgnoreCase("quit")) {
              throw new IOException()
            }
            outputFromClient.println(message)
          }
          catch {
            case e: IOException => {
              println(s"Quit ${e.getMessage}")
            }
            case e: SocketException => println(s"Quit ${e.getMessage}")
            case e: Exception => println("Unknown Error")
          }
        }
      } catch {
        case e: IOException => {
          outputFromClient.close()
          println(s"Client Quit ${e.getMessage}")
        }
      }
    }
  }
}
