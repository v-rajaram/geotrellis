package geotrellis.spark.io.cassandra

import geotrellis.spark._
import org.joda.time.{DateTime, DateTimeZone}

package object spacetime {
  private[spacetime] def timeChunk(time: DateTime): String =
    time.getYear.toString

  private[spacetime] def timeText(key: SpaceTimeKey): String =
    key.temporalKey.time.withZone(DateTimeZone.UTC).toString

  private[spacetime] def rowId(id: LayerId, index: Long): String =
    f"${id.zoom}%02d_${index}%019d"
}