
import akka.actor._;



object project3 {
  
  def main(args:Array[String]) = {
    
	  val numNodes = 1000
	  val numRequest = 3
	  val system = ActorSystem("Pastry")
	  val bossInstance = system.actorOf(Props(new Boss(numNodes,numRequest)),"BossInstance");
	  bossInstance ! Boss.start
	  
	  system.awaitTermination()
    
  }
  

}