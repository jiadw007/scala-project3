
import akka.actor.Actor
import scala.collection.mutable
import akka.actor.ActorRef
import akka.actor.Props
object Boss{
  
	var count = 0l
	var sum = 0l
	case class finished(hop:Long)
	case class done(index:Int)
	case object start
	
	var i = 1
  
  
}

class Boss(numNodes:Int = 1000, numRequest: Int = 1) extends Actor {
  
	var randomPool = new mutable.ListBuffer[Int]()
	var status = Array.fill(numNodes)(false)
	var init = Array.fill(numNodes)(false)
	
	val nodeList = new Array[ActorRef](numNodes)
	nodeList(0) = context.system.actorOf(Props(new Node(0,numRequest,new NodeId(0))),"NodeInstance0")
	randomPool.append(0)
	randomPool.append(Int.MaxValue)
	
	def receive : Actor.Receive = {
	  
	  case Boss.finished(hop) =>{
	    
	    Boss.count += 1
	    Boss.sum += hop
	    
	    
	  }
	  
	  case Boss.done(index) =>{
	    
	    this.status(index) = true
	    if(this.status.forall(p=>p)){
	      
	      println(1.0 *Boss.sum/ Boss.count)
	      sys.exit(0)
	      
	    }
	    
	    
	  }
	  
	  case Boss.start =>{
	    
	    val i = Boss.i
	    var nid = NodeId.randomNodeId()
	    while(randomPool.contains(nid.bits)) nid = NodeId.randomNodeId()
	    randomPool.append(nid.bits)
	    
	    nodeList(i) = context.system.actorOf(Props(new Node(i,numRequest, nid)),s"NodeInstance$i")
	    nodeList(i) ! Node.initial
	    
	    
	    
	    
	  }
	  
	  case Node.initialized =>{
	    
	    Boss.i +=1
	    if(Boss.i <=numNodes -1)
	      
	      self ! Boss.start
	      
	    else
	      
	      nodeList.foreach(_!Node.start)
	    
	    
	  }
	  
	  
	  
	  
	  
	  
	}
	
}