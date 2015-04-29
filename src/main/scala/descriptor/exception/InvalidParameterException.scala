package descriptor.exception

/**
 * Created by Junya on 15/04/29.
 */
class InvalidParameterException(msg: String) extends Exception {
  override def getMessage: String = msg
}
