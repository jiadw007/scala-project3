
import akka.actor._;



object project3 {
  
  def main(args:Array[String]) = {
    
	  val numNodes = args(0).toInt
	  val numRequest = args(1).toInt
	  val system = ActorSystem("Pastry")
	  val bossInstance = system.actorOf(Props(new Boss(numNodes,numRequest)),"BossInstance");
	  bossInstance ! Boss.start
	  
	  system.awaitTermination()
    
  }
  

}