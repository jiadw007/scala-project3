import scala.util.Random


object NodeId{
  
  def randomNodeId() = new NodeId(math.abs(Random.nextInt(Int.MaxValue)))
  
}

class NodeId(var bits: Int, implicit val b:Int =2) extends Ordered[NodeId]{
	
  
  val base = math.pow(2,b).toInt
  
  def compare(that:NodeId): Int = math.signum(this.bits - that.bits).toInt
  
  def getCommonPrefixLength(that:NodeId) : Int = this.toString().zip(that.toString()).takeWhile(Function.tupled(_ ==_)).unzip._1.length
 
  def getDistance(that: NodeId): Int = math.abs(this.bits - that.bits)
  
  override def toString() = "%16s".format(java.lang.Integer.toString(this.bits, this.base)).replace(' ', '0')
}