package model

import java.net.InetAddress

/**
 * Created by Junya on 15/05/07.
 */
class ServentInfo(val port: Short, val ip: InetAddress,
                   val numberOfFilesShared: Int,
                   val numberOfKilobytesShared: Int,
                   val hops: Int) {
}
