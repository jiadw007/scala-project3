
import scala.collection.mutable
import akka.actor.Actor
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
object Node{
  
  var mapNodeActor = new mutable.HashMap[Int, Int]
  
  case object pong
  case object initial
  case object initialized
  case object start
  case class join(key: NodeId, index: Int, hop: mutable.ListBuffer[NodeId], response: Boolean) //Node joins network with NodeId = key
  case class route(msg: (Int,Int), key: NodeId) // count of hops
  case class deliver(msg: (Int,Int), key: NodeId) //delivered to the target
  case class forward(mag: (Int,Int), key: NodeId, next: NodeId)
  case class newLeafs(nodeId: NodeId, leafSet: (mutable.ListBuffer[NodeId],mutable.ListBuffer[NodeId]), hop:mutable.ListBuffer[NodeId], response: Boolean)
  case class newRouting(routing: Array[Array[NodeId]], nodeId:NodeId, hop:mutable.ListBuffer[NodeId])
  case class newNeighbours(neighbor: mutable.ListBuffer[NodeId])
  case class updateRoutingEntry(nodeId:NodeId)
  
  
  
}


class Node(val index: Int, var numRequest: Int, var id: NodeId, val b: Int = 2,val L:Int =16) extends Actor with Ordered[Node] {
  
  s"Default configuration is ($b,$L)"
  val base = math.pow(2,b).toInt
  val rows = 32/b
  val cols = base
  var numFinished = 0
  var numSent = 0
  
  if(id==null){
    
    this.id = NodeId.randomNodeId()
    
  }
  
  var leafSet = (new mutable.ListBuffer[NodeId], new mutable.ListBuffer[NodeId])
  
  var routingTable =Array.ofDim[NodeId](rows,cols)
  for(i <-0 to rows - 1){
    
    routingTable(i)(this.id.toString()(i).toString.toInt) = new NodeId(-999)
    
    
    
  }
  val neighborSet = new mutable.ListBuffer[NodeId]
  
  //Ordered interface
  
  def compare(that: Node): Int = {
    
    this.id.compare(that.id)
    
  }
  
  def findActorRef(nodeId: NodeId) = context.actorSelection(s"/user/NodeInstance${Node.mapNodeActor(nodeId.bits)}")
  def UpdateActorRef(nodeId: NodeId, index: Int) = {Node.mapNodeActor(nodeId.bits) = index}
  UpdateActorRef(this.id,this.index)
  
  def UpdateLeaf(key: NodeId): Boolean = {
    
    if(this.leafSet._1.contains(key) || this.leafSet._2.contains(key))
      
      return true
    if(key < this.id){
      
      this.leafSet._1.append(key)
      if(this.leafSet._1.length > L / 2){
        
        this.leafSet = (this.leafSet._1.sortBy(p=> p.getDistance(this.id)).slice(0,L/2), this.leafSet._2)
        
      }
    }
    if(key > this.id){
      
      this.leafSet._2.append(key)
      if(leafSet._2.length > L / 2){
        
        this.leafSet = (this.leafSet._1, this.leafSet._2.sortBy(p => p.getDistance(this.id)).slice(0,L/2))
        
      } 
    }
    return this.leafSet._1.contains(key) || this.leafSet._2.contains(key)
    
  }
  
  def receive: Actor.Receive= {
    
    case Node.route(msg, key) =>{
      
      val fullSet = (this.leafSet._1.toList ::: this.leafSet._2.toList).sorted
      val minDistance = fullSet.map(p => p.getDistance(key)).min
      
      if(key.bits >= fullSet.map(p=>p.bits).min && key.bits <= fullSet.map(p =>p.bits).max){
        
        val endNode = fullSet(fullSet.indexWhere(p=>p.getDistance(key) == minDistance))
        
        if(this.id.getDistance(key) <= endNode.getDistance(key)){
          
          self ! Node.deliver(msg, key)
          
          
        }else{
          
          findActorRef(endNode) ! Node.deliver((msg._1 + 1,msg._2),key)
          
        }
      }else{
        
        val shl = key.getCommonPrefixLength(this.id)
        val row =shl
        val col = key.toString()(shl).toString().toInt
        //check if next is in routingTable
        
        if(routingTable(row)(col) != null){
          
          findActorRef(routingTable(row)(col)) ! Node.route((msg._1 +1 ,msg._2), key)
          
        }else{
          
          assert ((this.leafSet._1.toList ::: this.leafSet._2.toList).length >0)
          val fullSet = (this.leafSet._1.toList ::: this.leafSet._2.toList ::: this.neighborSet.toList) :+ this.id
          var minDistance = fullSet.map(p => p.getDistance(key)).min
          var endNode = fullSet(fullSet.indexWhere(p=> p.getDistance(key) == minDistance))
          for( i <- 0 to rows-1; j<- 0 to cols -1){
            
            if(this.routingTable(i)(j) != null && this.routingTable(i)(j).bits != -999 && this.routingTable(i)(j).getDistance(key)< minDistance){
              
              minDistance = this.routingTable(i)(j).getDistance(key)
              endNode = this.routingTable(i)(j)
              
              
            }
            
          }
          if(endNode.bits ==this.id.bits){
            
            self ! Node.deliver(msg,key)
            
          }else{
            
            findActorRef(endNode) ! Node.route((msg._1 +1, msg._2),key)
            
          }
        }
      }  
    }
    
    case Node.deliver(msg,key) =>{
      
      context.actorSelection("/user/BossInstance") ! Boss.finished(msg._1)
      
      context.actorSelection("/user/NodeInstance${msg._2}") ! Node.pong
      
      
      
    }
    
    case Node.join(key, index, hop, response) =>{
      
      hop.append(this.id)
      
      val inleaf = this.UpdateLeaf(key)
      
      if(inleaf){
        
        context.actorSelection(s"/user/NodeInstance$index") ! Node.newLeafs(this.id , this.leafSet, hop, response)
        
      }else{
        
        val shl = key.getCommonPrefixLength(this.id)
        val row = shl
        val col = key.toString()(shl).toString().toInt
        if(this.routingTable(row)(col) ==null){
          
          val fullSet = (this.leafSet._1.toList ::: this.leafSet._2.toList :::this.neighborSet.toList).sorted :+ this.id
          var minDistance = fullSet.map(p => p.getDistance(key)).min
          var endNode = fullSet(fullSet.indexWhere(p=> p.getDistance(key) == minDistance))
          for( i <- 0 to rows-1; j<- 0 to cols -1){
            
            if(this.routingTable(i)(j) != null && this.routingTable(i)(j).bits != -999 && this.routingTable(i)(j).getDistance(key)< minDistance){
              
              minDistance = this.routingTable(i)(j).getDistance(key)
              endNode = this.routingTable(i)(j)
              
              
            }
            
          } 
          routingTable(row)(col) = key
          findActorRef(endNode) ! Node.join(key,index,hop,response)
          
          context.actorSelection(s"/user/NodeInstance$index") ! Node.newRouting(this.routingTable, this.id, hop)
          
          
          
        }else{
          
          findActorRef(routingTable(row)(col)) ! Node.join(key,index,hop,response)
          context.actorSelection(s"/user/NodeInstance$index") ! Node.newRouting(this.routingTable, this.id, hop)
          
        } 
      }
    }
    
    case Node.pong => {
      
      this.numFinished +=1
      if(this.numFinished == this.numRequest){
        
        context.actorSelection("/user/BossInstance") ! Boss.done(this.index)
        
      }
    
    }
    
    case Node.initial =>{
      
      val n =Random.nextInt(this.index)
      context.actorSelection(s"/user/NodeInstance0") ! Node.join(this.id, this.index, new mutable.ListBuffer[NodeId](),true)
      
      
    }
    case Node.newLeafs(nodeId, leafSet2, hop,response) =>{
      
      ((leafSet2._1.toList :+ nodeId)::: leafSet2._2.toList).foreach(this.UpdateLeaf(_))
      hop.foreach(findActorRef(_) ! Node.updateRoutingEntry(this.id))
      hop.foreach(findActorRef(_) ! Node.newRouting(this.routingTable, this.id, new mutable.ListBuffer[NodeId]))
      for (i <- 0 to rows-1 ;j<- 0 to cols-1){
        
        val entry = this.routingTable(i)(j)
        if(entry !=null && entry.bits != -999){
          
          findActorRef(entry) ! Node.newRouting(this.routingTable, this.id, new mutable.ListBuffer[NodeId])
          
        }
      } 
      if(response) context.actorSelection("/user/BossInstance") ! Node.initialized
      
      
      
    }
    
    case Node.newRouting(routingTable2, nodeId, hop) =>{
      
      for(i <- 0 to rows - 1; j<- 0 to cols-1){
        
        if(this.routingTable(i)(j) ==null &&routingTable(i)(j) !=null &&routingTable(i)(j) != -999 && this.id.getCommonPrefixLength(routingTable2(i)(j)) >= i){
          
          this.routingTable(i)(j) ==routingTable2(i)(j)
          
        }
        
        
      }
      
      
    }
    
    case Node.start =>{
      
      val target = math.abs(Random.nextInt())
      self ! Node.route((0, this.index) , new NodeId(target))
      this.numSent += 1
      if(this.index ==0){
        
        println(s"Round ${this.numSent}")
        
      }
      
      if(this.numSent < this.numRequest){
        context.system.scheduler.scheduleOnce( 1000 milliseconds, self, Node.start)
                
      }
      
      
    }
    
    case Node.updateRoutingEntry(nodeId) =>{
      
      val shl = this.id.getCommonPrefixLength(nodeId)
      val col = nodeId.toString()(shl).toString().toInt
      if(this.routingTable(shl)(col) == null)
        this.routingTable(shl)(col) = nodeId
      
    }
    case _ =>{
      
      println("something received")
    }
    
    
    
    
    
    
    
    
    
  }
  

}